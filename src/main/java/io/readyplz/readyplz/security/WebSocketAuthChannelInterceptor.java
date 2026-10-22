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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    static final String USER_NOTIFICATION_DESTINATION = "/user/queue/notifications";

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.isHeartbeat()) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnect(accessor);
            return message;
        }
        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
            return message;
        }
        if (StompCommand.SEND.equals(command)) {
            // 메시지 본문은 HTTP로 저장한다. 클라이언트의 브로커·앱 목적지 SEND는 허용하지 않는다.
            log.warn("STOMP SEND 거절: user={}, destination={}", principalName(accessor.getUser()), accessor.getDestination());
            throw new AccessDeniedException("STOMP send denied");
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
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
                accessor.setUser(auth);
            } else {
                throw new IllegalStateException("Invalid token");
            }
        } catch (Exception e) {
            log.warn("WebSocket CONNECT 인증 실패: {}", e.getMessage());
            throw new IllegalStateException("WebSocket authentication failed");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!isAuthenticatedPrincipal(accessor.getUser()) || !USER_NOTIFICATION_DESTINATION.equals(destination)) {
            log.warn("STOMP SUBSCRIBE 거절: user={}, destination={}", principalName(accessor.getUser()), destination);
            throw new AccessDeniedException("STOMP subscription denied");
        }
    }

    private boolean isAuthenticatedPrincipal(Principal principal) {
        if (principal == null) {
            return false;
        }
        if (principal instanceof Authentication authentication && !authentication.isAuthenticated()) {
            return false;
        }
        String name = principal.getName();
        return name != null && !name.isBlank() && !"anonymousUser".equals(name);
    }

    private String principalName(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return "anonymous";
        }
        return principal.getName();
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


