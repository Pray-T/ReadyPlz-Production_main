package io.readyplz.readyplz.service;

import io.readyplz.readyplz.config.MessageRetentionProperties;
import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.Message;
import io.readyplz.readyplz.dto.notification.NotificationDTO;
import io.readyplz.readyplz.dto.summary.ConversationSummaryDTO;
import io.readyplz.readyplz.repository.MemberGameRepository;
import io.readyplz.readyplz.repository.MemberRepository;
import io.readyplz.readyplz.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

	private static final String NOTIFICATION_DESTINATION = "/queue/notifications";
	private static final String MESSAGE_NOTIFICATION_TYPE = "MESSAGE";
	private static final String MESSAGE_NOTIFICATION_TEXT = "새 메시지가 도착했습니다.";

	private final MessageRepository messageRepository;
	private final MemberRepository memberRepository;
	private final MemberGameRepository memberGameRepository;
	private final MessageRetentionProperties messageRetentionProperties;
	private final SimpMessagingTemplate messagingTemplate;

	@Transactional
	public Message sendMessage(Long senderId, Long receiverId, String content) {
		String trimmed = content == null ? "" : content.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("메시지 내용을 입력해주세요.");
		}
		if (trimmed.length() > 1000) {
			throw new IllegalArgumentException("메시지는 1000자 이하여야 합니다.");
		}

		assertCanSendDirectMessage(senderId, receiverId);

		Member sender = memberRepository.findById(Objects.requireNonNull(senderId))
				.orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));
		Member receiver = memberRepository.findById(Objects.requireNonNull(receiverId))
				.orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));

		ensureConversationMessageLimit(senderId, receiverId);

		Message message = Message.create(sender, receiver, trimmed);
		Message saved = messageRepository.save(Objects.requireNonNull(message));

		scheduleMessageNotificationAfterCommit(receiver.getUsername(), sender.getNickname());
		return saved;
	}

	/**
	 * DB 커밋 성공 후에만 STOMP 알림을 발행한다.
	 * 발행 실패는 메시지 저장/HTTP 응답에 영향을 주지 않는다.
	 */
	private void scheduleMessageNotificationAfterCommit(String receiverUsername, String senderNickname) {
		NotificationDTO payload = new NotificationDTO(
				MESSAGE_NOTIFICATION_TYPE,
				MESSAGE_NOTIFICATION_TEXT,
				senderNickname);

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publishMessageNotification(receiverUsername, payload);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publishMessageNotification(receiverUsername, payload);
			}
		});
	}

	private void publishMessageNotification(String receiverUsername, NotificationDTO payload) {
		try {
			messagingTemplate.convertAndSendToUser(receiverUsername, NOTIFICATION_DESTINATION, payload);
			log.info("실시간 메시지 알림 발행: receiver={}, type={}", receiverUsername, payload.type());
		} catch (Exception e) {
			log.error("실시간 메시지 알림 발행 실패: receiver={}", receiverUsername, e);
		}
	}

	public void assertCanViewConversation(Long memberId, Long otherMemberId) {
		if (memberId.equals(otherMemberId)) {
			throw new IllegalArgumentException("자기 자신과는 대화할 수 없습니다.");
		}
		Member other = memberRepository.findById(Objects.requireNonNull(otherMemberId))
				.orElseThrow(() -> new IllegalArgumentException("대화 상대를 찾을 수 없습니다."));
		if (hasExistingConversation(memberId, otherMemberId)) {
			return;
		}
		if (memberGameRepository.existsSharedGame(memberId, otherMemberId)) {
			return;
		}
		if (hasAdminRole(other)) {
			return;
		}
		throw new IllegalArgumentException("대화에 접근할 권한이 없습니다.");
	}

	public void assertCanSendDirectMessage(Long senderId, Long receiverId) {
		if (senderId.equals(receiverId)) {
			throw new IllegalArgumentException("자기 자신에게는 메시지를 보낼 수 없습니다.");
		}
		memberRepository.findById(Objects.requireNonNull(receiverId))
				.orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));
		if (hasExistingConversation(senderId, receiverId)) {
			return;
		}
		if (memberGameRepository.existsSharedGame(senderId, receiverId)) {
			return;
		}
		Member receiver = memberRepository.findById(receiverId).orElseThrow();
		if (hasAdminRole(receiver)) {
			return;
		}
		throw new IllegalArgumentException("메시지를 보낼 권한이 없습니다. 같은 게임을 보유한 사용자에게만 보낼 수 있습니다.");
	}

	public Page<io.readyplz.readyplz.dto.summary.MessageSummaryDTO> getConversation(Long memberId1, Long memberId2, Pageable pageable) {
		return messageRepository.findConversation(memberId1, memberId2, pageable);
	}

	public Page<ConversationSummaryDTO> getConversations(Long userId, Pageable pageable) {
		Page<Object[]> page = messageRepository.findConversationsForUser(userId, pageable);
		return page.map(this::mapConversationRow);
	}

	public boolean isConversationLimitReached(Long memberId1, Long memberId2) {
		long count = messageRepository.countConversationBetween(memberId1, memberId2);
		return count >= messageRetentionProperties.getMaxPerConversation();
	}

	private boolean hasExistingConversation(Long memberId1, Long memberId2) {
		return messageRepository.countConversationBetween(memberId1, memberId2) > 0;
	}

	private boolean hasAdminRole(Member member) {
		return member.getRoles().stream()
				.anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
	}

	private ConversationSummaryDTO mapConversationRow(Object[] row) {
		Long otherMemberId = row[0] == null ? null : ((Number) row[0]).longValue();
		String otherMemberNickname = row[1] == null ? null : row[1].toString();
		String lastMessageContent = row[2] == null ? null : row[2].toString();
		LocalDateTime lastMessageTime = null;
		if (row[3] instanceof LocalDateTime ldt) {
			lastMessageTime = ldt;
		} else if (row[3] instanceof Timestamp ts) {
			lastMessageTime = ts.toLocalDateTime();
		} else if (row[3] != null) {
			lastMessageTime = LocalDateTime.parse(row[3].toString());
		}
		boolean isLastFromMe;
		if (row[4] instanceof Boolean b) {
			isLastFromMe = b;
		} else if (row[4] instanceof Number n) {
			isLastFromMe = n.intValue() != 0;
		} else {
			isLastFromMe = Boolean.parseBoolean(String.valueOf(row[4]));
		}
		long unreadCount = row[5] == null ? 0L : ((Number) row[5]).longValue();
		return new ConversationSummaryDTO(otherMemberId, otherMemberNickname, lastMessageContent, lastMessageTime, isLastFromMe, unreadCount);
	}

	private void ensureConversationMessageLimit(Long senderId, Long receiverId) {
		long count = messageRepository.countConversationBetween(senderId, receiverId);
		if (count >= messageRetentionProperties.getMaxPerConversation()) {
			int batch = messageRetentionProperties.getTrimBatchSize();
			Pageable trimPage = PageRequest.of(0, batch,
					Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
			Page<Long> oldestIds = messageRepository.findOldestMessageIdsInConversation(senderId, receiverId, trimPage);
			if (!oldestIds.getContent().isEmpty()) {
				messageRepository.deleteAllByIdInBatch(oldestIds.getContent());
			}
		}
	}
}
