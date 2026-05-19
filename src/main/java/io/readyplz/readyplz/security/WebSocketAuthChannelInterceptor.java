package io.readyplz.readyplz.security;

import io.readyplz.readyplz.util.JwtTokenUtil;
import io.readyplz.readyplz.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwt = resolveJwt(accessor);
            if (jwt == null || jwt.isBlank()) {
                log.warn("WebSocket CONNECT에 유효한 access token이 없습니다.");
                throw new IllegalStateException("Missing access token");
            }
            try {
                if (tokenService.isBlacklisted(jwt)) {
                    throw new IllegalStateException("Blacklisted token");
                }

                String username = jwtTokenUtil.extractUsername(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtTokenUtil.validateToken(jwt, userDetails)
                        && tokenService.isActiveAccessToken(username, jwt)) {
                    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    accessor.setUser(auth);
                } else {
                    throw new IllegalStateException("Invalid token");
                }
            } catch (Exception e) {
                log.warn("WebSocket CONNECT 인증 실패: {}", e.getMessage());
                throw new IllegalStateException("WebSocket authentication failed");
            }
        }

        return message;
    }

    private String resolveJwt(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object token = sessionAttributes.get(WebSocketCookieHandshakeInterceptor.ACCESS_TOKEN_SESSION_ATTR);
            if (token instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }
}


