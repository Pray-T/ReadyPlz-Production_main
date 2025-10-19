package Readyplz.io.ReadyPlz.service;

import Readyplz.io.ReadyPlz.domain.Member;
import Readyplz.io.ReadyPlz.domain.Message;
import Readyplz.io.ReadyPlz.dto.SummaryDTO.ConversationSummaryDTO;
import Readyplz.io.ReadyPlz.repository.MemberRepository;
import Readyplz.io.ReadyPlz.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
 
 

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final MemberRepository memberRepository;
 

    // 메시지 전송
    @Transactional
    public Message sendMessage(Long senderId, Long receiverId, String content) {
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));

        // 메시지 저장 한도 확인 및 자동 삭제
        ensureMessageLimit();

        Message message = Message.create(sender, receiver, content);

        return messageRepository.save(message);
    }

    // 보낸 메시지 목록 조회 (페이지네이션)
    public Page<Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO> getSentMessages(Long memberId, Pageable pageable) {
        return messageRepository.findBySenderId(memberId, pageable);
    }

    // 받은 메시지 목록 조회 (페이지네이션)
    public Page<Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO> getReceivedMessages(Long memberId, Pageable pageable) {
        return messageRepository.findByReceiverId(memberId, pageable);
    }

    // 대화 내용 조회 (페이지네이션)
    public Page<Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO> getConversation(Long memberId1, Long memberId2, Pageable pageable) {
        return messageRepository.findConversation(memberId1, memberId2, pageable);
    }

    // 사용자별 대화방 목록 조회 (상대 사용자 기준 그룹핑)
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
  
    // 메시지 저장 한도 확인
    public boolean isMessageLimitReached() {
        return messageRepository.countTotalMessages() >= 1000;
    }
    
    // 메시지 저장 한도 확인 및 자동 삭제
    @Transactional
    public void ensureMessageLimit() {
        Long currentCount = messageRepository.countTotalMessages();
        if (currentCount >= 1000) {
            // 한도에 도달했을 때 가장 오래된 메시지 10개 삭제 (여유 공간 확보)
            int deleteCount = 10;
            messageRepository.deleteOldestMessages(deleteCount);
        }
    }
    
} 