package com.pulsepass.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public class JwtProperties {

    private String issuer;
    private String secretBase64;
    private Duration accessTokenTtl;

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getSecretBase64() { return secretBase64; }
    public void setSecretBase64(String secretBase64) { this.secretBase64 = secretBase64; }

    public Duration getAccessTokenTtl() { return accessTokenTtl; }
    public void setAccessTokenTtl(Duration accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
}