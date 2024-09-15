package com.example.ai01.user.service;

import com.example.ai01.metrics.service.GrafanaService;
import com.example.ai01.security.util.JwtUtil;
import com.example.ai01.user.dto.request.MemberRequest;
import com.example.ai01.user.dto.response.MemberResponse;
import com.example.ai01.user.entity.Member;
import com.example.ai01.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;



@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final GrafanaService grafanaService;

    @Value("${server.ip}")
    private String serverIp;
    @Value("${cost.groq}")
    private double groqCost;

    @Value("${cost.vllm}")
    private double vllmCost;




    @Transactional(readOnly = true)
    public MemberResponse.MemberInfo getMemberInfo(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Member not found with id: " + id));

        return MemberResponse.MemberInfo
                .builder()
                .id(id)
                .name(member.getName())
                .name(member.getUsername())
                .nickname(member.getNickname())
                .phone(member.getPhone())
                .email(member.getEmail())
                .build();
    }

    @Transactional(readOnly = true)
    public MemberResponse.MemberInfo getMemberInfoByUsername(String username) {
        // 유저 프로필 조회 로직
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return MemberResponse.MemberInfo
                .builder()
                .id(member.getId())
                .name(member.getName())
                .name(member.getUsername())
                .nickname(member.getNickname())
                .phone(member.getPhone())
                .email(member.getEmail())
                .build();
    }


    @Transactional
    public void createMember(MemberRequest.CreateMember request) {
        Member member = Member.DTOtoEntity(request);
        memberRepository.save(member);

    }


    @Transactional
    public String signUp(MemberRequest.SignUpRequest request) {
        // 회원가입 로직 구현
        if (memberRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 새로운 회원 생성 및 저장
        Member member = new Member();
        member.setUsername(request.getUsername());
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        memberRepository.save(member);


        // Grafana에서 조직 생성, 사용자 추가
        grafanaService.createGrafanaUser(member.getUsername(), member.getEmail(), request.getPassword());

        String orgName = member.getUsername() + "_org";
        int orgId = grafanaService.createOrganization(orgName);
        log.info("orgId: {}", orgId);
        grafanaService.addUserToOrganization(member.getUsername(), orgId);

        // 데이터 소스 생성
        String dataSourceJson = getDataSourceJson();
        grafanaService.addDataSourceToOrganization(orgId, dataSourceJson);

        // 대시보드 생성
        String dashboardJson = getDashboardJson(member.getUsername());
        log.info("dashboardJson: {}", dashboardJson);
        grafanaService.createDashboardForOrganization(orgId, dashboardJson);


        return "User registered successfully";
    }


    @Transactional
    public String login(MemberRequest.LoginRequest request) {
        // 로그인 로직 구현
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        // 로그인 성공 시 JWT 토큰 발급
        return jwtUtil.generateToken(member.getUsername());
    }

    private String getDashboardJson(String username) {

        double total_cost = vllmCost + groqCost;

        // 각 패널은 경로별 요청 수와 전체 요청 수를 시각화하고, 대시보드의 크기도 사용자가 보기 편리하도록 조정함
        String data = "{"
                + "\"dashboard\": {"
                + "    \"id\": null,"
                + "    \"uid\": null,"
                + "    \"title\": \"User API Requests\","
                + "    \"timezone\": \"browser\","
                + "    \"schemaVersion\": 16,"
                + "    \"version\": 0,"
                + "    \"panels\": ["
                + "        {"
                + "            \"type\": \"graph\","
                + "            \"title\": \"Total Requests for All Paths\","
                + "            \"gridPos\": { \"x\": 0, \"y\": 0, \"w\": 24, \"h\": 8 },"
                + "            \"targets\": ["
                + "                {"
                + "                    \"expr\": \"sum by (path) (http_server_requests_user_total{user_id=\\\"" + username + "\\\", path=~\\\"/api/groq/complete|/api/vllm/complete\\\"})\","
                + "                    \"legendFormat\": \"Total Requests for all paths\""
                + "                },"
                + "                {"
                + "                    \"expr\": \"sum(http_server_requests_user_total{user_id=\\\"" + username + "\\\", path=~\\\"/api/groq/complete|/api/vllm/complete\\\"}) * " + total_cost + "\","
                + "                    \"legendFormat\": \"Total Cost for all paths\","
                + "                    \"refId\": \"B\""
                + "                }"
                + "            ]"
                + "        },"
                + "        {"
                + "            \"type\": \"graph\","
                + "            \"title\": \"Requests to /api/groq/complete\","
                + "            \"gridPos\": { \"x\": 0, \"y\": 8, \"w\": 24, \"h\": 8 },"
                + "            \"targets\": ["
                + "                {"
                + "                    \"expr\": \"sum(http_server_requests_user_total{user_id=\\\"" + username + "\\\", path=\\\"/api/groq/complete\\\"})\","
                + "                    \"legendFormat\": \"/api/groq/complete\""
                + "                },"
                + "                {"
                + "                    \"expr\": \"sum(http_server_requests_user_total{user_id=\\\"" + username + "\\\", path=\\\"/api/groq/complete\\\"}) * " + groqCost + "\","
                + "                    \"legendFormat\": \"Cost for /api/groq/complete\","
                + "                    \"refId\": \"B\""
                + "                }"
                + "            ]"
                + "        },"
                + "        {"
                + "            \"type\": \"graph\","
                + "            \"title\": \"Requests to /api/vllm/complete\","
                + "            \"gridPos\": { \"x\": 0, \"y\": 16, \"w\": 24, \"h\": 8 },"
                + "            \"targets\": ["
                + "                {"
                + "                    \"expr\": \"sum(http_server_requests_user_total{user_id=\\\"" + username + "\\\", path=\\\"/api/vllm/complete\\\"})\","
                + "                    \"legendFormat\": \"/api/vllm/complete\""
                + "                },"
                + "                {"
                + "                    \"expr\": \"sum(http_server_requests_user_total{user_id=\\\"" + username + "\\\", path=\\\"/api/vllm/complete\\\"}) * " + vllmCost + "\","
                + "                    \"legendFormat\": \"Cost for /api/vllm/complete\","
                + "                    \"refId\": \"B\""
                + "                }"
                + "            ]"
                + "        }"
                + "    ]"
                + "},"
                + "\"overwrite\": true"
                + "}";

        log.info("generated data: " + data);
        return data;
    }



    private String getDataSourceJson() {
        return "{"
                + "\"name\": \"Prometheus\"," // datasource 이름은 모두 Prometheus 로 일단 동일하게 생성
                + "\"type\": \"prometheus\","
                + "\"url\": \"http://" + serverIp + ":9090\","
                + "\"access\": \"proxy\","
                + "\"basicAuth\": false"
                + "}";
    }








}
