package com.toodback.auth;

import com.toodback.member.Member;
import com.toodback.member.MemberRepository;
import com.toodback.member.MemberResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class AuthController {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;


    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(LoginFailedException::new);

        if (!passwordEncoder.matches(request.getPassword(), member.getEncodedPassword())) {
            throw new LoginFailedException();
        }

        String accessToken = tokenProvider.createToken(member.getId());

        return new LoginResponse(accessToken);
    }

    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal Member member) {
        return MemberResponse.from(member);
    }
}