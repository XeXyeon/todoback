package com.toodback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ToodbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToodbackApplication.class, args);
    }

}
