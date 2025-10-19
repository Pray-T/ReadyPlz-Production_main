package Readyplz.io.ReadyPlz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // 액세스 토큰 저장 (Redis TTL 설정)
    public void saveAccessToken(String username, String token, long expirationTime) {
        String key = ACCESS_TOKEN_PREFIX + username;
        redisTemplate.opsForValue().set(key, token, expirationTime, TimeUnit.MILLISECONDS);
        // log.debug("액세스 토큰 저장: {}", username);
    }

    // 리프레시 토큰 저장
    public void saveRefreshToken(String username, String token, long expirationTime) {
        String key = REFRESH_TOKEN_PREFIX + username;
        redisTemplate.opsForValue().set(key, token, expirationTime, TimeUnit.MILLISECONDS);
        // log.debug("리프레시 토큰 저장: {}", username);
    }

    // 액세스 토큰 조회
    public String getAccessToken(String username) {
        String key = ACCESS_TOKEN_PREFIX + username;
        return redisTemplate.opsForValue().get(key);
    }

    // 리프레시 토큰 조회
    public String getRefreshToken(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        return redisTemplate.opsForValue().get(key);
    }

    // 토큰 블랙리스트 추가 (로그아웃 시)
    public void addToBlacklist(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", expirationTime, TimeUnit.MILLISECONDS);
        // log.debug("토큰 블랙리스트 추가: {}", token.substring(0, Math.min(20, token.length())) + "..."); 
    }

    // 토큰이 블랙리스트에 있는지 확인 = 무효화 토큰인지 확인 
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // 사용자의 모든 토큰 삭제 (로그아웃 시)
    public void deleteUserTokens(String username) {
        String accessKey = ACCESS_TOKEN_PREFIX + username;
        String refreshKey = REFRESH_TOKEN_PREFIX + username;
        
        redisTemplate.delete(java.util.Arrays.asList(accessKey, refreshKey));
        // log.debug("사용자 토큰 삭제: {}", username);
    }

    // 액세스 토큰 삭제
    public void deleteAccessToken(String username) {
        String key = ACCESS_TOKEN_PREFIX + username;
        redisTemplate.delete(key);
        // log.debug("액세스 토큰 삭제: {}", username);
    }

    // 리프레시 토큰 삭제
    public void deleteRefreshToken(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        redisTemplate.delete(key);
        // log.debug("리프레시 토큰 삭제: {}", username);
    }
} 