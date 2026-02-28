package com.emar.order_app;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenTest {

    @Test
    void printHash() {
        String raw = "123456"; // istediğin şifre
        System.out.println(new BCryptPasswordEncoder().encode(raw));
    }
}
