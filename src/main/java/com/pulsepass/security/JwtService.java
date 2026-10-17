package com.pulsepass.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = buildSigningKey(properties);
    }

    @PostConstruct
    void validateConfigurationOnStartup() {
        if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
            throw new IllegalStateException("pulsepass.security.jwt.issuer must be configured");
        }
        if (properties.getAccessTokenTtl() == null || properties.getAccessTokenTtl().isZero()) {
            throw new IllegalStateException("pulsepass.security.jwt.access-token-ttl must be configured");
        }
    }

    private SecretKey buildSigningKey(JwtProperties properties) {
        String secretBase64 = properties.getSecretBase64();
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "PULSEPASS_JWT_SECRET_BASE64 is not set. The application refuses to start without it.");
        }
        byte[] decoded = Decoders.BASE64.decode(secretBase64);
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "pulsepass.security.jwt.secret-base64 must decode to at least " + MIN_SECRET_BYTES + " bytes");
        }
        return Keys.hmacShaKeyFor(decoded);
    }

    public String generateToken(Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessTokenTtl());

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getAccessTokenTtl().getSeconds();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new MalformedJwtException("Token subject (sub) is missing");
        }
        return Long.valueOf(subject);
    }
}