package Readyplz.io.ReadyPlz.dto.request;

import Readyplz.io.ReadyPlz.validation.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PasswordMatches(message = "password_input_error", newPasswordField = "password", confirmPasswordField = "confirmPassword")
public class DeleteAccountRequest {

    @NotBlank(message = "email_input_error")
    @Email(message = "email_input_error")
    private String email;

    @NotBlank(message = "password_input_error")
    @Size(min = 6, message = "password_input_error")
    private String password;

    @NotBlank(message = "password_input_error")
    private String confirmPassword;
}


