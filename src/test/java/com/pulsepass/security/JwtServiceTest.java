package com.pulsepass.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET = "cHVsc2VwYXNzLXRlc3Qtc2VjcmV0LWtleS0yMDI2ISE=";

    private JwtService service(String issuer, Duration ttl) {
        JwtProperties props = new JwtProperties();
        props.setIssuer(issuer);
        props.setSecretBase64(VALID_SECRET);
        props.setAccessTokenTtl(ttl);
        return new JwtService(props);
    }

    @Test
    void generatesAndParsesValidToken() {
        JwtService jwtService = service("pulsepass-api", Duration.ofMinutes(15));

        String token = jwtService.generateToken(42L);
        Long userId = jwtService.parseUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void rejectsTokenWithWrongIssuer() {
        JwtService issuerA = service("issuer-a", Duration.ofMinutes(15));
        JwtService issuerB = service("issuer-b", Duration.ofMinutes(15));

        String token = issuerA.generateToken(1L);

        assertThatThrownBy(() -> issuerB.parseUserId(token)).isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwtService = service("pulsepass-api", Duration.ofMillis(1));

        String token = jwtService.generateToken(1L);

        await(5);

        assertThatThrownBy(() -> jwtService.parseUserId(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwtService = service("pulsepass-api", Duration.ofMinutes(15));
        String token = jwtService.generateToken(1L);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parseUserId(tampered)).isInstanceOf(SignatureException.class);
    }

    @Test
    void r3t24NpUrJMNunMMASmhAM953bFGeLXzN7() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setIssuer("pulsepass-api");
        otherProps.setSecretBase64("b3RoZXItdGVzdC1zZWNyZXQta2V5LTIwMjYtMzJieXRlcyE=");
        otherProps.setAccessTokenTtl(Duration.ofMinutes(15));
        JwtService other = new JwtService(otherProps);

        JwtService mine = service("pulsepass-api", Duration.ofMinutes(15));
        String token = other.generateToken(1L);

        assertThatThrownBy(() -> mine.parseUserId(token)).isInstanceOf(SignatureException.class);
    }

    @Test
    void rejectsWeakSecret() {
        JwtProperties props = new JwtProperties();
        props.setIssuer("pulsepass-api");
        props.setSecretBase64("dG9vLXNob3J0");
        props.setAccessTokenTtl(Duration.ofMinutes(15));

        assertThatThrownBy(() -> new JwtService(props)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingSecret() {
        JwtProperties props = new JwtProperties();
        props.setIssuer("pulsepass-api");
        props.setAccessTokenTtl(Duration.ofMinutes(15));

        assertThatThrownBy(() -> new JwtService(props)).isInstanceOf(IllegalStateException.class);
    }

    private void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
