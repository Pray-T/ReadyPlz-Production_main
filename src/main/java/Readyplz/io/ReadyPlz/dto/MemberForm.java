package Readyplz.io.ReadyPlz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class MemberForm {
    
    @NotBlank(message = "아이디는 필수 입력값입니다.")
    @Size(min = 4, max = 16, message = "아이디는 4~16자 사이여야 합니다.")
    private String username;

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
    private String password;
    
    @NotBlank(message = "비밀번호 확인은 필수 입력값입니다.")
    private String passwordConfirm;

    @NotBlank(message = "국가는 필수입니다.")
    private String country;

    @Size(max = 12, message = "닉네임은 12자를 초과할 수 없습니다.")
    @Pattern(regexp = "^\\S+$", message = "닉네임에는 공백을 사용할 수 없습니다.")
    private String nickname;
} 