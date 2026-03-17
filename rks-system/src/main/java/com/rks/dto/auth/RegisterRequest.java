package com.rks.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String passwordConfirm;
    private String name;
}
