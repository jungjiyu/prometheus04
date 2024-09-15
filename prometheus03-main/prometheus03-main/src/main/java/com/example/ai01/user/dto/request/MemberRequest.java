package com.example.ai01.user.dto.request;

import lombok.*;

public class MemberRequest {


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateMember {
        private String name;
        private String password;
        private String username;
        private String nickname;
        private String email;
        private String phone;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest  {
        private String username;
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignUpRequest  {
        private String username;
        private String password;
        private String email;
    }


}
