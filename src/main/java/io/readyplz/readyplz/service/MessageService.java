package io.readyplz.readyplz.service;

import io.readyplz.readyplz.config.MessageRetentionProperties;
import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.Message;
import io.readyplz.readyplz.dto.summary.ConversationSummaryDTO;
import io.readyplz.readyplz.repository.MemberRepository;
import io.readyplz.readyplz.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

	private final MessageRepository messageRepository;
	private final MemberRepository memberRepository;
	private final MessageRetentionProperties messageRetentionProperties;

	@Transactional
	public Message sendMessage(Long senderId, Long receiverId, String content) {
		Member sender = memberRepository.findById(Objects.requireNonNull(senderId))
				.orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));
		Member receiver = memberRepository.findById(Objects.requireNonNull(receiverId))
				.orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));

		ensureConversationMessageLimit(senderId, receiverId);

		Message message = Message.create(sender, receiver, content);

		return messageRepository.save(Objects.requireNonNull(message));
	}

	public Page<io.readyplz.readyplz.dto.summary.MessageSummaryDTO> getSentMessages(Long memberId, Pageable pageable) {
		return messageRepository.findBySenderId(memberId, pageable);
	}

	public Page<io.readyplz.readyplz.dto.summary.MessageSummaryDTO> getReceivedMessages(Long memberId, Pageable pageable) {
		return messageRepository.findByReceiverId(memberId, pageable);
	}

	public Page<io.readyplz.readyplz.dto.summary.MessageSummaryDTO> getConversation(Long memberId1, Long memberId2, Pageable pageable) {
		return messageRepository.findConversation(memberId1, memberId2, pageable);
	}

	public Page<ConversationSummaryDTO> getConversations(Long userId, Pageable pageable) {
		Page<Object[]> page = messageRepository.findConversationsForUser(userId, pageable);
		return page.map(this::mapConversationRow);
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

	/** 현재 사용자와 상대 간 대화 메시지 수가 설정 상한 이상인지 */
	public boolean isConversationLimitReached(Long memberId1, Long memberId2) {
		long count = messageRepository.countConversationBetween(memberId1, memberId2);
		return count >= messageRetentionProperties.getMaxPerConversation();
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
