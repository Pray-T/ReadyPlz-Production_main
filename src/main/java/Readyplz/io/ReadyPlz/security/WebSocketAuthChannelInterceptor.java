package Readyplz.io.ReadyPlz.security;

import Readyplz.io.ReadyPlz.util.JwtTokenUtil;
import Readyplz.io.ReadyPlz.service.TokenService;
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
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    // 블랙리스트 토큰 거부
                    if (tokenService.isBlacklisted(jwt)) {
                        throw new IllegalStateException("Blacklisted token");
                    }

                    String username = jwtTokenUtil.extractUsername(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtTokenUtil.validateToken(jwt, userDetails)) {
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
            } else {
                log.warn("WebSocket CONNECT에 Authorization 헤더가 없습니다.");
                throw new IllegalStateException("Missing Authorization header");
            }
        }

        return message;
    }
}


