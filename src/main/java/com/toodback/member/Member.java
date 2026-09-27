package com.toodback.member;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String encodedPassword;
    @Column(nullable = false)
    private String nickname;
    private LocalDateTime createdAt;

    protected Member() {
    }

    public Member(String email, String encodedPassword, String nickname) {
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now();
    }

}
