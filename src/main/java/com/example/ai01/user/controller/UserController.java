package com.example.ai01.user.controller;


import com.example.ai01.user.dto.response.UserResponse;
import com.example.ai01.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final JwtUtil jwtUtil;

    @PostMapping("/token")
    public  ResponseEntity<UserResponse.Basic> generateToken(@RequestHeader("Private-Key") String privateKey, @RequestParam String username) throws Exception {
        try {
            // Generate JWT using the private key and username
            String jwtToken = jwtUtil.generateToken(username, privateKey);
            return ResponseEntity.ok(UserResponse.Basic.builder().token(jwtToken).build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(UserResponse.Basic.builder().token("").build());
        }
    }


}
