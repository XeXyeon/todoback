package com.toodback.auth;

import com.toodback.member.Member;
import com.toodback.member.MemberRepository;
import com.toodback.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, MemberRepository memberRepository,
                                   ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.memberRepository = memberRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!authorizationHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Invalid authorization header");
            }
            Long memberId = tokenProvider.getMemberId(authorizationHeader.substring(7));
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(LoginFailedException::new);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(member, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException | LoginFailedException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ErrorResponse.of(401, "유효한 인증 토큰이 필요합니다."));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
