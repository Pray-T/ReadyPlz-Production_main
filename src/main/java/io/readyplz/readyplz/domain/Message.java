package io.readyplz.readyplz.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message",
       indexes = {
           @Index(name = "idx_message_sender_created_at", columnList = "sender_id,created_at"),
           @Index(name = "idx_message_receiver_created_at", columnList = "receiver_id,created_at"),
           @Index(name = "idx_message_receiver_is_read_created_at", columnList = "receiver_id,is_read,created_at"),
           @Index(name = "idx_message_sender_receiver_created_at", columnList = "sender_id,receiver_id,created_at"),
           @Index(name = "idx_message_receiver_sender_created_at", columnList = "receiver_id,sender_id,created_at")
       }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"sender", "receiver"})
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sender_id", nullable = true)
    private Member sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    @Column(nullable = false, length = 1000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    // 메시지 읽음 상태 업데이트
    public void markAsRead() {
        this.isRead = true;
    }

    // FK는 sender/receiver에 있다. sentMessages/receivedMessages는 mappedBy 역방향이라
    // 여기서 contains/add 하지 않는다. 그 호출은 LAZY 컬렉션을 초기화해 회원 전체 이력을 SELECT한다.
    public static Message create(Member sender, Member receiver, String content) {
        Message message = new Message();
        message.sender = sender;
        message.receiver = receiver;
        message.content = content;
        return message;
    }

   
} 