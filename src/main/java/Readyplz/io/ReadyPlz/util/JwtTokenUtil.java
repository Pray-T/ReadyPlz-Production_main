package Readyplz.io.ReadyPlz.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.access-token-validity}")
    private Long accessTokenValidity;

    @Value("${jwt.refresh-token-validity}")
    private Long refreshTokenValidity;
    
    // JWT Claim Keys
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TYPE = "type";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //클레임 추출 로직. Claim이란?: 토큰에 담기는 정보의 조각들입니다 (예: 사용자 이름, 권한, 만료 시간) 페이로드와 유사.
    //extractAllClaims로 얻어온 전체 클레임 덩어리에서 원하는 특정 클레임 하나만 추출하는 로직
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ROLES, userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername(), accessTokenValidity);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TYPE, TOKEN_TYPE_REFRESH);
        return createToken(claims, userDetails.getUsername(), refreshTokenValidity);
    }

    private String createToken(Map<String, Object> claims, String subject, Long validity) {
        return Jwts.builder()
                .claims(claims) 
                .subject(subject) //토큰 주체
                .issuedAt(new Date(System.currentTimeMillis())) //발행시간
                .expiration(new Date(System.currentTimeMillis() + validity))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact(); 
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE));
        } catch (Exception e) {
            return false;
        }
    }
} 