package com.example.ai01.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Component
public class JwtUtil {


    @Value("${jwt.expiration}")
    private long expirationTime;  // 토큰 만료 시간 (밀리초 단위)

    private PublicKey publicKey;

    // 생성자에서 public_key.pem 파일을 읽어와 공개 키 초기화
    public JwtUtil() throws Exception {
        // 공개 키 로드
        String publicKeyPath = "keys/public_key.pem";
        InputStream publicKeyStream = getClass().getClassLoader().getResourceAsStream(publicKeyPath);
        if (publicKeyStream == null) {
            throw new IllegalArgumentException("Public key file not found: " + publicKeyPath);
        }
        String publicKeyContent = new String(publicKeyStream.readAllBytes(), StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyContent);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.publicKey = keyFactory.generatePublic(publicKeySpec);
    }

    // JWT 생성 메서드 (클라이언트로부터 받은 privateKey 사용)
    public String generateToken(String username, String privateKeyString) throws Exception {
        PrivateKey privateKey = loadPrivateKeyFromString(privateKeyString);
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, privateKey);
    }

    // privateKey String을 PrivateKey 객체로 변환
    private PrivateKey loadPrivateKeyFromString(String privateKeyString) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(
                privateKeyString
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "")
        );
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(privateKeySpec);
    }

    // 실제 JWT 생성 및 서명
    private String createToken(Map<String, Object> claims, String subject, PrivateKey privateKey) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(privateKey, SignatureAlgorithm.RS256)  // 클라이언트에서 받은 private key로 서명
                .compact();
    }

    // JWT에서 모든 클레임 추출
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)  // 공개 키를 이용해 서명 검증
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // JWT에서 사용자명 추출
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // JWT 만료 여부 확인
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // 토큰 검증 (서명 및 사용자명 확인)
    public boolean validateToken(String token, String username) {
        return (username.equals(extractUsername(token)) && !isTokenExpired(token));
    }
}