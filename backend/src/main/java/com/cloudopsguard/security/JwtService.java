package com.cloudopsguard.security;

import com.cloudopsguard.config.JwtProperties;
import com.cloudopsguard.domain.common.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * access トークン（JWT・HS256）の発行と検証（jjwt 0.12.6 API）。
 * 認可に必要な userId / role / username をクレームに載せる（クライアント入力を信用しない）。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlSeconds;

    public JwtService(JwtProperties props) {
        byte[] secretBytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            // HS256 は 256bit 以上必須。短い秘密は起動時に弾く（fail-fast）。
            throw new IllegalStateException("app.jwt.secret は 32 文字（256bit）以上にしてください");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessTtlSeconds = props.accessTtlSeconds();
    }

    /** access トークンを発行する（sub=userId・role・username）。 */
    public String issueAccessToken(Long userId, String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * access トークンを検証して principal を取り出す。
     * 署名不正・期限切れ等は {@code JwtException} 系で失敗（呼び出し側で握って未認証扱い）。
     */
    public AppUserPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        Role role = Role.valueOf(claims.get("role", String.class));
        String username = claims.get("username", String.class);
        return new AppUserPrincipal(userId, username, role);
    }
}
