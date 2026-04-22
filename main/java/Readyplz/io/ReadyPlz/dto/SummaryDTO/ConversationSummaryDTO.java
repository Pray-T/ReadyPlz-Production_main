package Readyplz.io.ReadyPlz.dto.SummaryDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDTO {
    private Long otherMemberId;
    private String otherMemberNickname;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private boolean isLastMessageFromMe;
    private long unreadCount;
}
