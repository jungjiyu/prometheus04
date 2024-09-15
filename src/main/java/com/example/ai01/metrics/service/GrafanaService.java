package com.example.ai01.metrics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class GrafanaService {


    private final RestTemplate restTemplate;

    @Value("${server.ip}")
    private String serverIp;

    @Value("${grafana.admin.username}")
    private String adminUsername;

    @Value("${grafana.admin.password}")
    private String adminPassword;

    // 조직별 API 키를 저장하기 위한 Map
    private final Map<Integer, String> orgApiKeys = new HashMap<>();

    public int createOrganization(String orgName) {
        String url = UriComponentsBuilder.fromHttpUrl("http://"+serverIp + ":3000/api/orgs")
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(adminUsername, adminPassword);  // admin 계정의 기본 인증 설정
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("name", orgName);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        // 응답 로그로 찍어보기
        log.info("Response from Grafana create organization API: {}", response.getBody());

        // JSON 응답에서 조직 ID 추출
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(response.getBody());
            int orgId = rootNode.path("orgId").asInt();
            log.info("Parsed orgId: {}", orgId);

            // 조직 생성 후, 서비스 계정 및 API 키 생성
            String apiKey = createServiceAccountAndApiKey(orgId, orgName);
            orgApiKeys.put(orgId, apiKey); // 생성한 API 키를 Map에 저장

            return orgId;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Grafana response", e);
            throw new RuntimeException("Failed to parse Grafana response", e);
        }
    }

    // 서비스 계정 생성 및 API 토큰 생성
    private String createServiceAccountAndApiKey(int orgId, String orgName) {

        // Switch the org context for the Admin user to the new org:
        String switchConetextUrl = UriComponentsBuilder.fromHttpUrl("http://" + serverIp + ":3000/api/user/using/" + orgId)
                .encode()
                .toUriString();

        log.info("switchConetextUrl: " + switchConetextUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(adminUsername, adminPassword); // Basic Auth 설정
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentType(MediaType.APPLICATION_JSON);


        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(null, headers); //  request body가 없을 경우에는 HttpEntity를 생성할 때 body 부분에 null을 넣어준다.
        ResponseEntity<String> switchConetextResponse = restTemplate.postForEntity(switchConetextUrl, requestEntity, String.class);


        String responseBody = switchConetextResponse.getBody();
        log.info("정상적으로 admin 으로 조직의 컨텍스트가 전환됨: " + responseBody); // JSON 응답을 그대로 로그로 출력


        // Create a Service Account
        String createServiceAccountUrl = UriComponentsBuilder.fromHttpUrl("http://" + serverIp + ":3000/api/serviceaccounts")
                .encode()
                .toUriString();


        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBasicAuth(adminUsername, adminPassword); // Basic Auth 설정
        headers2.setContentType(MediaType.APPLICATION_JSON);
        headers2.setContentType(MediaType.APPLICATION_JSON);


        Map<String, Object> body = new HashMap<>();
        body.put("name", orgName + "-service-account");
        body.put("role", "Admin"); // 서비스 계정에 할당할 역할 설정


        HttpEntity<Map<String, Object>> requestEntity2 = new HttpEntity<>(body, headers2);
        ResponseEntity<String> serviceAccountResponse = restTemplate.postForEntity(createServiceAccountUrl, requestEntity2, String.class);
        String responseBody2 = serviceAccountResponse.getBody();
        log.info("정상적으로 서비스 계정이 생성됨: " + responseBody2); // JSON 응답을 그대로 로그로 출력


        // 응답 파싱 및 서비스 id get
        ObjectMapper objectMapper2 = new ObjectMapper();
        Integer serviceAccountId = null;
        try {
            JsonNode rootNode = objectMapper2.readTree(serviceAccountResponse.getBody());
            serviceAccountId = rootNode.path("id").asInt();
            log.info("생성된 서비스 계정의 id: " + serviceAccountId);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse service account creation response", e);
            throw new RuntimeException("Failed to create service account", e);
        }


// Create a Service Account token
        String apiTokenUrl = UriComponentsBuilder.fromHttpUrl("http://" + serverIp + ":3000/api/serviceaccounts/" + serviceAccountId + "/tokens")
                .encode()
                .toUriString();

        log.info("apiTokenUrl: "+apiTokenUrl);

// 헤더 설정 (중복된 Content-Type 설정 제거)
        HttpHeaders headers3 = new HttpHeaders();
        headers3.setBasicAuth(adminUsername, adminPassword); // Basic Auth 설정
        headers3.setContentType(MediaType.APPLICATION_JSON);

// 요청 본문 설정 (name 필드만 전달)
        Map<String, Object> body3 = new HashMap<>();
        body3.put("name", "api-token-for-service-account"); // 공식 문서에 명시된 대로 이름만 포함

        HttpEntity<Map<String, Object>> requestEntity3 = new HttpEntity<>(body3, headers3);

        try {
            // POST 요청을 보내서 토큰 생성
            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(apiTokenUrl, requestEntity3, String.class);

            // 응답 파싱 및 로그 출력
            ObjectMapper objectMapper3 = new ObjectMapper();
            JsonNode rootNode = objectMapper3.readTree(tokenResponse.getBody());
            log.info("정상적으로 API Key가 생성됨: " + rootNode.toString());

            // 생성된 API Key 추출
            String apiKey = rootNode.path("key").asText();
            log.info("api key: " + apiKey);

            return apiKey; // API 키 반환
        } catch (JsonProcessingException e) {
            log.error("Failed to parse tokenResponse", e);
            throw new RuntimeException("Failed to parse tokenResponse", e);
        }
    }




    // 회원가입 시 입력한 username과 password를 사용하여 grafana 계정 생성
    public void createGrafanaUser(String username, String email, String password) {
        String url = UriComponentsBuilder.fromHttpUrl("http://"+serverIp + ":3000/api/admin/users")
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(adminUsername, adminPassword);

        Map<String, Object> body = new HashMap<>();
        body.put("name", username);
        body.put("email", email);
        body.put("login", username);
        body.put("password", password);
        body.put("OrgId", 1);  // 기본 조직 ID로 설정. 나중에 사용자를 특정 조직으로 이동시킬 수 있습니다.

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, requestEntity, String.class);
    }

    public void addUserToOrganization(String username, int orgId) {
        String url = UriComponentsBuilder.fromHttpUrl("http://"+serverIp + ":3000/api/orgs/" + orgId + "/users")
                .encode()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(adminUsername, adminPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("loginOrEmail", username);
        body.put("role", "Viewer");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, requestEntity, String.class);
    }

    public void createDashboardForOrganization(int orgId, String dashboardJson) {
        String url = UriComponentsBuilder.fromHttpUrl("http://"+serverIp + ":3000/api/dashboards/db")
                .encode()
                .toUriString();

        String apiKey = orgApiKeys.get(orgId); // 조직별 API 키 가져오기
        log.info("조직"+orgId+"의 api key를 가져옴:"+apiKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey); // 조직별 API 키 사용
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(dashboardJson, headers);
        restTemplate.postForEntity(url, requestEntity, String.class);
    }

    public void addDataSourceToOrganization(int orgId, String dataSourceJson) {
        String apiKey = orgApiKeys.get(orgId); // 조직별 API 키 가져오기
        log.info("조직 " + orgId + "의 api key를 가져옴: " + apiKey);

        String url = UriComponentsBuilder.fromHttpUrl("http://" + serverIp + ":3000/api/datasources")
                .encode()
                .toUriString();

        log.info("데이터 소스 생성 URL: " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey); // 조직별 API 키 사용
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<String> requestEntity = new HttpEntity<>(dataSourceJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            String result = response.getBody();
            log.info("데이터 소스를 생성함: " + result);

        } catch (Exception e) {
            log.error("Failed to create data source", e);
            throw new RuntimeException("Failed to create data source", e);
        }
    }
}
