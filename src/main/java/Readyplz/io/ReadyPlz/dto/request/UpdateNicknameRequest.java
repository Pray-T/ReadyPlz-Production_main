package Readyplz.io.ReadyPlz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateNicknameRequest {

    @NotBlank(message = "empty_nickname")
    @Size(max = 12, message = "invalid_nickname_length")
    @Pattern(regexp = "^\\S+$", message = "invalid_nickname_whitespace")
    private String nickname;
}


