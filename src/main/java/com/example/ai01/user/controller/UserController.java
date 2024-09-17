package com.example.ai01.user.controller;


import com.example.ai01.metrics.dto.response.PrometheusResponse;
import com.example.ai01.metrics.service.PrometheusService;
import com.example.ai01.user.dto.response.UserResponse;
import com.example.ai01.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final JwtUtil jwtUtil;
    private final PrometheusService prometheusService;

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



    @GetMapping("/usage")
    public ResponseEntity<PrometheusResponse.UsageMetrics> getJsonFormatUserUsage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName(); // 사용자 아이디 가져오기
        try {
            PrometheusResponse.UsageMetrics response = prometheusService.getJsonFormatUserUsage(userId);
            log.info(" created response for userId {}: {}", userId,response.toString());

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Error fetching usage metrics for userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



}
