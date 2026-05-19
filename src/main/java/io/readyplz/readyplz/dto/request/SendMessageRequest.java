package io.readyplz.readyplz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotNull(message = "수신자를 지정해주세요.")
    private Long receiverId;

    @NotBlank(message = "메시지 내용을 입력해주세요.")
    @Size(max = 1000, message = "메시지는 1000자 이하여야 합니다.")
    private String content;
}
