package io.readyplz.readyplz.security;

import io.readyplz.readyplz.service.TokenService;
import io.readyplz.readyplz.util.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    private static final Pattern USERNAME_IN_NOTIFICATION_PATH =
            Pattern.compile("/user/\\$\\{[^}]+\\}/queue/notifications");

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenService tokenService;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    @Test
    void subscribe_allowsAuthenticatedUsersOwnNotificationQueue() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, "/user/queue/notifications", user("alice"));

        Message<?> result = interceptor.preSend(message, channel);

        assertSame(message, result);
        assertEquals("alice", StompHeaderAccessor.wrap(result).getUser().getName());
        verifyNoInteractions(jwtTokenUtil, userDetailsService, tokenService);
    }

    @Test
    void subscribe_rejectsOtherUserQueueAndBrokerDestinations() {
        String[] denied = {
                "/user/other/queue/notifications",
                "/user/alice/queue/notifications",
                "/queue/notifications",
                "/topic/notifications",
                "/topic/games",
                "/user/queue/notifications/extra"
        };
        for (String destination : denied) {
            Message<byte[]> message = frame(StompCommand.SUBSCRIBE, destination, user("alice"));
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> interceptor.preSend(message, channel), destination);
            assertEquals("STOMP subscription denied", ex.getMessage());
        }

        Message<byte[]> missingDestination = frame(StompCommand.SUBSCRIBE, null, user("alice"));
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(missingDestination, channel));
    }

    @Test
    void subscribe_rejectsUnauthenticatedSession() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, "/user/queue/notifications", null);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void send_rejectsOtherUserNotificationDestination() {
        String[] denied = {
                "/user/other/queue/notifications",
                "/user/alice/queue/notifications",
                "/user/queue/notifications",
                "/queue/notifications",
                "/topic/notifications",
                "/app/chat"
        };
        for (String destination : denied) {
            Message<byte[]> message = frame(StompCommand.SEND, destination, user("alice"));
            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> interceptor.preSend(message, channel), destination);
            assertEquals("STOMP send denied", ex.getMessage());
        }
    }

    @Test
    void disconnectUnsubscribeAndHeartbeat_doNotDropTheSession() {
        Message<byte[]> disconnect = frame(StompCommand.DISCONNECT, null, user("alice"));
        Message<byte[]> unsubscribe = frame(StompCommand.UNSUBSCRIBE, "/user/queue/notifications", user("alice"));
        Message<byte[]> heartbeat = heartbeatFrame();

        assertSame(disconnect, interceptor.preSend(disconnect, channel));
        assertSame(unsubscribe, interceptor.preSend(unsubscribe, channel));
        assertSame(heartbeat, interceptor.preSend(heartbeat, channel));
        verifyNoInteractions(jwtTokenUtil, userDetailsService, tokenService);
    }

    @Test
    void connect_keepsUsernamePrincipalWhenTokenMatchesRedis() {
        String jwt = "token-value";
        stubActiveToken(jwt, "user1");
        Message<byte[]> message = connectFrame("Bearer " + jwt, null, user("user1"));

        Message<?> result = interceptor.preSend(message, channel);

        assertSame(message, result);
        Principal principal = StompHeaderAccessor.wrap(result).getUser();
        assertEquals("user1", principal.getName());
        assertTrue(principal instanceof Authentication);
        verify(tokenService).isBlacklisted(jwt);
        verify(jwtTokenUtil).extractUsername(jwt);
        verify(jwtTokenUtil).validateToken(jwt, userDetails("user1"));
        verify(tokenService).isActiveAccessToken("user1", jwt);
    }

    @Test
    void connect_acceptsHandshakeCookieTokenWhenItMatchesRedis() {
        String jwt = "cookie-token";
        stubActiveToken(jwt, "user1");
        Message<byte[]> message = connectFrame(null, jwt, user("user1"));

        Message<?> result = interceptor.preSend(message, channel);

        assertSame(message, result);
        assertEquals("user1", StompHeaderAccessor.wrap(result).getUser().getName());
        verify(tokenService).isBlacklisted(jwt);
        verify(tokenService).isActiveAccessToken("user1", jwt);
    }

    @Test
    void connect_rejectsBlacklistedToken() {
        String jwt = "token-value";
        when(tokenService.isBlacklisted(jwt)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> interceptor.preSend(connectFrame("Bearer " + jwt, null, null), channel));

        assertEquals("WebSocket authentication failed", ex.getMessage());
        verify(jwtTokenUtil, never()).extractUsername(jwt);
        verify(tokenService, never()).isActiveAccessToken("user1", jwt);
    }

    @Test
    void connect_rejectsTokenThatDoesNotMatchRedis() {
        String jwt = "token-value";
        UserDetails userDetails = userDetails("user1");
        when(tokenService.isBlacklisted(jwt)).thenReturn(false);
        when(jwtTokenUtil.extractUsername(jwt)).thenReturn("user1");
        when(userDetailsService.loadUserByUsername("user1")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(jwt, userDetails)).thenReturn(true);
        when(tokenService.isActiveAccessToken("user1", jwt)).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> interceptor.preSend(connectFrame("Bearer " + jwt, null, null), channel));

        assertEquals("WebSocket authentication failed", ex.getMessage());
        verify(tokenService).isActiveAccessToken("user1", jwt);
    }

    @Test
    void connect_rejectsMissingAccessToken() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> interceptor.preSend(connectFrame(null, null, null), channel));

        assertEquals("Missing access token", ex.getMessage());
        verifyNoInteractions(jwtTokenUtil, userDetailsService, tokenService);
    }

    @Test
    void clientSubscribesWithoutUsernameInNotificationPath() throws IOException {
        Path authJs = Path.of("src", "main", "resources", "static", "js", "auth.js");
        String authSource = Files.readString(authJs);
        assertTrue(authSource.contains("this.stompClient.subscribe('/user/queue/notifications'"));
        assertFalse(USERNAME_IN_NOTIFICATION_PATH.matcher(authSource).find());

        try (var paths = Files.walk(Path.of("src", "main", "resources"))) {
            List<Path> clients = paths.filter(path -> {
                String name = path.getFileName().toString();
                return name.endsWith(".js") || name.endsWith(".html");
            }).toList();
            for (Path client : clients) {
                String source = Files.readString(client);
                assertFalse(USERNAME_IN_NOTIFICATION_PATH.matcher(source).find(), client::toString);
            }
        }
    }

    private void stubActiveToken(String jwt, String username) {
        UserDetails userDetails = userDetails(username);
        when(tokenService.isBlacklisted(jwt)).thenReturn(false);
        when(jwtTokenUtil.extractUsername(jwt)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(jwt, userDetails)).thenReturn(true);
        when(tokenService.isActiveAccessToken(username, jwt)).thenReturn(true);
    }

    private UserDetails userDetails(String username) {
        return new User(username, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Authentication user(String username) {
        UserDetails userDetails = userDetails(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private Message<byte[]> frame(StompCommand command, String destination, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (principal != null) {
            accessor.setUser(principal);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> heartbeatFrame() {
        StompHeaderAccessor accessor = StompHeaderAccessor.createForHeartbeat();
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> connectFrame(String authorization, String sessionToken, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        if (sessionToken != null) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put(WebSocketCookieHandshakeInterceptor.ACCESS_TOKEN_SESSION_ATTR, sessionToken);
            accessor.setSessionAttributes(attributes);
        }
        if (principal != null) {
            accessor.setUser(principal);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
