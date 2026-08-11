package com.ocp.eia.infrastructure.security;

import com.ocp.eia.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String ACCESS_COOKIE = "eia_access";
    public static final String REFRESH_COOKIE = "eia_refresh";

    private final AppProperties appProperties;

    public void writeAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        long accessMaxAge = Math.max(1, appProperties.getJwt().getAccessExpirationMs() / 1000);
        long refreshMaxAge = Math.max(1, appProperties.getJwt().getRefreshExpirationMs() / 1000);
        boolean secure = appProperties.getCookie() != null && appProperties.getCookie().isSecure();

        response.addHeader("Set-Cookie", buildCookie(ACCESS_COOKIE, accessToken, accessMaxAge, secure).toString());
        response.addHeader("Set-Cookie", buildCookie(REFRESH_COOKIE, refreshToken, refreshMaxAge, secure).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        boolean secure = appProperties.getCookie() != null && appProperties.getCookie().isSecure();
        response.addHeader("Set-Cookie", buildCookie(ACCESS_COOKIE, "", 0, secure).toString());
        response.addHeader("Set-Cookie", buildCookie(REFRESH_COOKIE, "", 0, secure).toString());
    }

    public String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds, boolean secure) {
        return ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
