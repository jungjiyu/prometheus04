package com.example.ai01.security.filter;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class CustomFilter extends OncePerRequestFilter {

    @Value("${cost.groq}")
    private double groqCost;

    @Value("${cost.vllm}")
    private double vllmCost;

    @Value("${cost.azure}")
    private double azureCost;

    @Value("${cost.openai}")
    private double openaiCost;

    private final MeterRegistry meterRegistry;

    public CustomFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // /metrics 및 /actuator 경로를 필터에서 제외
        if (!path.startsWith("/metrics") && !path.startsWith("/actuator") && !path.startsWith("/api/prometheus/usage")) {
            log.info("Filtering request to path: {}", path);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = "anonymous"; // 기본값으로 anonymous 설정

            if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
                Object principal = authentication.getPrincipal();

                if (principal instanceof UserDetails) {
                    userId = ((UserDetails) principal).getUsername();
                } else if (principal != null) {
                    userId = principal.toString();
                }

                log.info("Registering metric for user_id: {}", userId);
            } else {
                log.warn("Authentication is null or not authenticated");
            }

            filterChain.doFilter(request, response); // 사용자 인증이나 요청 정보 미리 준비 후 status 같은거 get

            int status = response.getStatus();
            double cost = getCostForPath(path);

            // 메트릭 수집
            meterRegistry.counter("http.server.requests.user",
                    "user_id", userId,
                    "method", method,
                    "status", String.valueOf(status),
                    "path", path,
                    "cost", String.valueOf(cost) // 비용을 라벨로 추가
            ).increment();
        } else {
            filterChain.doFilter(request, response);
        }
    }

    // 특정 path에 따른 비용을 결정하는 로직
    private double getCostForPath(String path) {

        // 정규표현식 비교에는 equals 가 아닌 matches 사용 필요
        if (path.matches("/api/groq/.*"))  return groqCost;
        else if (path.matches("/api/vllm/.*")) return vllmCost;
        else if (path.matches("/api/openai/.*")) return openaiCost;
        else if (path.matches("/api/azure/.*")) return azureCost;

        return 0.0; // 다른 경로는 비용이 없다고 가정
    }}

