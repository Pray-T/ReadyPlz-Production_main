package Readyplz.io.ReadyPlz.dto.SummaryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSummaryDTO {
    private Long messageId;
    private String content;
    private LocalDateTime createdAt;
    private boolean read;

    private Long senderId;
    private String senderNickname;

    private Long receiverId;
    private String receiverNickname;
}


