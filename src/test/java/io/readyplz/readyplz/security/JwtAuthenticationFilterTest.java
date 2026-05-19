package io.readyplz.readyplz.security;

import io.readyplz.readyplz.service.TokenService;
import io.readyplz.readyplz.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenService tokenService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void rejectsTokenNotMatchingRedis() throws Exception {
        String jwt = "token-value";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenService.isBlacklisted(jwt)).thenReturn(false);
        when(jwtTokenUtil.extractUsername(jwt)).thenReturn("user1");
        var userDetails = new User("user1", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("user1")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(jwt, userDetails)).thenReturn(true);
        when(tokenService.isActiveAccessToken("user1", jwt)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void acceptsActiveRedisToken() throws Exception {
        String jwt = "token-value";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenService.isBlacklisted(jwt)).thenReturn(false);
        when(jwtTokenUtil.extractUsername(jwt)).thenReturn("user1");
        var userDetails = new User("user1", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername("user1")).thenReturn(userDetails);
        when(jwtTokenUtil.validateToken(jwt, userDetails)).thenReturn(true);
        when(tokenService.isActiveAccessToken("user1", jwt)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
