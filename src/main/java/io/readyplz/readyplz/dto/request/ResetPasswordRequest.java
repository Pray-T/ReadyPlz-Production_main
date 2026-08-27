package io.readyplz.readyplz.dto.request;

import io.readyplz.readyplz.validation.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PasswordMatches(
        message = "비밀번호가 일치하지 않습니다.",
        newPasswordField = "password",
        confirmPasswordField = "passwordConfirm"
)
public class ResetPasswordRequest {

    @NotBlank(message = "유효하지 않거나 만료된 토큰입니다.")
    private String token;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력값입니다.")
    private String passwordConfirm;
}
