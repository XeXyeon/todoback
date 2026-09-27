package com.toodback.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {

    @Email(message = "이메일 형식에 맞지 않습니다.")
    @NotBlank(message = "이메일은 필수 입력항목입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력항목입니다.")
    private String password;

}
