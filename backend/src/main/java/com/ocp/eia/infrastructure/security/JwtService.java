package com.ocp.eia.infrastructure.security;

import com.ocp.eia.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtService {

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public UUID extractJti(String token) {
        String jti = extractClaim(token, Claims::getId);
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("Missing jti");
        }
        return UUID.fromString(jti);
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserDetails userDetails, String role, String nomPrenom) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("nomPrenom", nomPrenom);
        claims.put("type", "access");
        return buildToken(claims, userDetails.getUsername(), appProperties.getJwt().getAccessExpirationMs(), null);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        UUID jti = UUID.randomUUID();
        return buildToken(claims, userDetails.getUsername(), appProperties.getJwt().getRefreshExpirationMs(), jti);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs, UUID jti) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        var builder = Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry);
        if (jti != null) {
            builder.id(jti.toString());
        }
        return builder.signWith(getSigningKey()).compact();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        String secret = appProperties.getJwt().getSecret();
        byte[] keyBytes = secret.length() >= 32
                ? secret.getBytes(StandardCharsets.UTF_8)
                : Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
