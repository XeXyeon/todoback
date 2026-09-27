package com.toodback.auth;

public class LoginFailedException extends RuntimeException {

    public LoginFailedException() {
        super("Email or password is invalid");
    }
}