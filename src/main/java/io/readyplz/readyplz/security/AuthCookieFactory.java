package io.readyplz.readyplz.security;

import io.readyplz.readyplz.config.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 로그인·회원가입 자동 로그인·갱신·로그아웃·세션 종료가 같은 쿠키 속성을 쓰게 한다.
 * Secure는 {@link HttpServletRequest#isSecure()}가 true일 때만 붙인다.
 */
@Component
@RequiredArgsConstructor
public class AuthCookieFactory {

    static final String ACCESS_TOKEN = "accessToken";
    static final String REFRESH_TOKEN = "refreshToken";
    private static final String SAME_SITE_LAX = "Lax";

    private final JwtProperties jwtProperties;

    public String[] sessionCookies(HttpServletRequest request, String accessToken, String refreshToken) {
        boolean secure = request.isSecure();
        return new String[] {
                cookie(ACCESS_TOKEN, accessToken, secure, jwtProperties.getAccessTokenValidity() / 1000).toString(),
                cookie(REFRESH_TOKEN, refreshToken, secure, jwtProperties.getRefreshTokenValidity() / 1000).toString()
        };
    }

    public String[] expiredSessionCookies(HttpServletRequest request) {
        boolean secure = request.isSecure();
        return new String[] {
                cookie(ACCESS_TOKEN, "", secure, 0).toString(),
                cookie(REFRESH_TOKEN, "", secure, 0).toString()
        };
    }

    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        for (String header : expiredSessionCookies(request)) {
            response.addHeader(HttpHeaders.SET_COOKIE, header);
        }
    }

    private static ResponseCookie cookie(String name, String value, boolean secure, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE_LAX)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
