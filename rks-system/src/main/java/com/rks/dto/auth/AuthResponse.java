package com.rks.dto.auth;

import lombok.Builder;
import lombok.Data;
import com.rks.entity.User.UserRole;

@Data
@Builder
public class AuthResponse {
    private String token;
    private UserResponse user;

    @Data
    @Builder
    public static class UserResponse {
        private String id;
        private String email;
        private String name;
        private UserRole role;
    }
}
