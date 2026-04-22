package Readyplz.io.ReadyPlz.dto.request;

import Readyplz.io.ReadyPlz.validation.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PasswordMatches(message = "password_input_error")
public class ChangePasswordRequest {

    @NotBlank(message = "password_input_error")
    private String currentPassword;

    @NotBlank(message = "password_input_error")
    @Size(min = 6, message = "password_input_error")
    private String newPassword;

    @NotBlank(message = "password_input_error")
    private String confirmPassword;
}


