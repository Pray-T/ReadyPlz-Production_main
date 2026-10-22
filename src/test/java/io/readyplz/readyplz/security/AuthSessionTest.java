package io.readyplz.readyplz.security;

import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.repository.MemberRepository;
import io.readyplz.readyplz.service.EmailService;
import io.readyplz.readyplz.service.TokenService;
import io.readyplz.readyplz.support.TestRedisConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestRedisConfig.class, AuthSessionTest.ForwardedHeadersMockMvcConfig.class})
class AuthSessionTest {

    private static final String PASSWORD = "password1";
    private static final String NEW_PASSWORD = "newpass12";
    private static final String PASSWORD_CHANGED_MSG = "비밀번호가 변경되었습니다. 다시 로그인해 주세요.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private EmailService emailService;

    @SpyBean
    private AuthCookieFactory authCookieFactory;

    private final Map<String, String> redisStore = new ConcurrentHashMap<>();
    private final AtomicReference<String> failDeleteUsername = new AtomicReference<>();
    private static final AtomicInteger SEQUENCE = new AtomicInteger(1000);

    @BeforeEach
    void stubRedis() {
        redisStore.clear();
        failDeleteUsername.set(null);
        clearInvocations(authCookieFactory);

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        doAnswer(invocation -> {
            redisStore.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(values.get(anyString())).thenAnswer(invocation -> redisStore.get(invocation.getArgument(0)));
        when(redisTemplate.hasKey(anyString())).thenAnswer(invocation -> redisStore.containsKey(invocation.getArgument(0)));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Collection<String> keys = invocation.getArgument(0);
            String blocked = failDeleteUsername.get();
            if (blocked != null && keys.stream().anyMatch(key -> key.endsWith(":" + blocked))) {
                throw new IllegalStateException("redis delete failed");
            }
            long removed = 0;
            for (String key : keys) {
                if (redisStore.remove(key) != null) {
                    removed++;
                }
            }
            return removed;
        }).when(redisTemplate).delete(anyCollection());
    }

    @Test
    void forwardHeadersStrategyRegistersFrameworkFilter() {
        assertTrue(frameworkFilterRegistered());
    }

    @Test
    void changePassword_revokesOnlyThatUserAndRejectsOldRefresh() throws Exception {
        Session changed = registerUser();
        Session other = registerUser();
        clearInvocations(authCookieFactory);

        MvcResult result = changePassword(changed, PASSWORD, NEW_PASSWORD, false, null)
                .andExpect(status().isFound())
                .andExpect(redirectsTo("/members/loginForm"))
                .andExpect(flash().attribute("msg", PASSWORD_CHANGED_MSG))
                .andReturn();

        assertExpiredAuthCookies(authSetCookies(result), false);
        assertNull(tokenService.getAccessToken(changed.username()));
        assertNull(tokenService.getRefreshToken(changed.username()));
        assertEquals(other.accessToken(), tokenService.getAccessToken(other.username()));
        assertEquals(other.refreshToken(), tokenService.getRefreshToken(other.username()));
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, passwordOf(changed.username())));

        verify(authCookieFactory).expireSession(any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(authCookieFactory, never()).sessionCookies(any(HttpServletRequest.class), anyString(), anyString());

        mockMvc.perform(post("/api/auth/refresh").cookie(changed.refreshCookie()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/members/profile").cookie(changed.accessCookie()))
                .andExpect(status().isFound())
                .andExpect(redirectsTo("/members/loginForm"));
        mockMvc.perform(post("/api/auth/refresh").cookie(other.refreshCookie()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/members/loginForm").flashAttr("msg", PASSWORD_CHANGED_MSG))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(PASSWORD_CHANGED_MSG)));
    }

    @Test
    void changePassword_expiresCookiesUsingRequestSecureFlag() throws Exception {
        Session httpsUser = registerUser();
        MvcResult https = changePassword(httpsUser, PASSWORD, NEW_PASSWORD, true, null)
                .andExpect(redirectsTo("/members/loginForm"))
                .andReturn();
        assertExpiredAuthCookies(authSetCookies(https), true);

        Session proxied = registerUser();
        MvcResult forwarded = changePassword(proxied, PASSWORD, NEW_PASSWORD, false, "https")
                .andExpect(redirectsTo("/members/loginForm"))
                .andReturn();
        assertExpiredAuthCookies(authSetCookies(forwarded), true);
    }

    @Test
    void changePassword_wrongPasswordKeepsTokens() throws Exception {
        Session session = registerUser();
        clearInvocations(authCookieFactory);

        changePassword(session, "wrong-pass", NEW_PASSWORD, false, null)
                .andExpect(redirectsTo("/members/profile"))
                .andExpect(flash().attribute("error", "password_input_error"));

        assertEquals(session.accessToken(), tokenService.getAccessToken(session.username()));
        assertEquals(session.refreshToken(), tokenService.getRefreshToken(session.username()));
        assertTrue(passwordEncoder.matches(PASSWORD, passwordOf(session.username())));
        verify(authCookieFactory, never()).expireSession(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void changePassword_doesNotCommitWhenTokenDeleteFails() throws Exception {
        Session session = registerUser();
        failDeleteUsername.set(session.username());
        clearInvocations(authCookieFactory);

        MvcResult result = changePassword(session, PASSWORD, NEW_PASSWORD, false, null)
                .andExpect(redirectsTo("/members/profile"))
                .andExpect(flash().attribute("error", "password_change_failed"))
                .andReturn();

        assertTrue(authSetCookies(result).stream().noneMatch(cookie -> cookie.contains("Max-Age=0")));
        assertTrue(passwordEncoder.matches(PASSWORD, passwordOf(session.username())));
        assertEquals(session.accessToken(), tokenService.getAccessToken(session.username()));
        assertEquals(session.refreshToken(), tokenService.getRefreshToken(session.username()));
        verify(authCookieFactory, never()).expireSession(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void resetPassword_revokesOnlyThatUserAndRejectsOldRefresh() throws Exception {
        Session reset = registerUser();
        Session other = registerUser();
        String token = "reset-" + reset.username();
        assignResetToken(reset.username(), token);
        clearInvocations(authCookieFactory);

        MvcResult result = resetPassword(reset, token, false, null)
                .andExpect(status().isFound())
                .andExpect(redirectsTo("/members/loginForm"))
                .andExpect(flash().attribute("msg", PASSWORD_CHANGED_MSG))
                .andReturn();

        assertExpiredAuthCookies(authSetCookies(result), false);
        assertNull(tokenService.getAccessToken(reset.username()));
        assertNull(tokenService.getRefreshToken(reset.username()));
        assertEquals(other.accessToken(), tokenService.getAccessToken(other.username()));
        assertEquals(other.refreshToken(), tokenService.getRefreshToken(other.username()));
        assertTrue(passwordEncoder.matches(NEW_PASSWORD, passwordOf(reset.username())));

        mockMvc.perform(post("/api/auth/refresh").cookie(reset.refreshCookie()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/members/profile").cookie(reset.accessCookie()))
                .andExpect(status().isFound())
                .andExpect(redirectsTo("/members/loginForm"));

        verify(authCookieFactory).expireSession(any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(authCookieFactory, never()).sessionCookies(any(HttpServletRequest.class), anyString(), anyString());
    }

    @Test
    void resetPassword_expiresSecureCookiesAndDoesNotCommitWhenDeleteFails() throws Exception {
        Session secure = registerUser();
        String secureToken = "reset-" + secure.username();
        assignResetToken(secure.username(), secureToken);
        MvcResult https = resetPassword(secure, secureToken, false, "https")
                .andExpect(redirectsTo("/members/loginForm"))
                .andReturn();
        assertExpiredAuthCookies(authSetCookies(https), true);

        Session failed = registerUser();
        String failedToken = "reset-" + failed.username();
        assignResetToken(failed.username(), failedToken);
        failDeleteUsername.set(failed.username());
        clearInvocations(authCookieFactory);

        MvcResult result = resetPassword(failed, failedToken, false, null)
                .andExpect(redirectsTo("/members/reset-password"))
                .andReturn();

        assertTrue(authSetCookies(result).stream().noneMatch(cookie -> cookie.contains("Max-Age=0")));
        assertTrue(passwordEncoder.matches(PASSWORD, passwordOf(failed.username())));
        assertEquals(failedToken, memberRepository.findByUsername(failed.username()).orElseThrow().getResetToken());
        assertEquals(failed.accessToken(), tokenService.getAccessToken(failed.username()));
        verify(authCookieFactory, never()).expireSession(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void registerAutoLogin_matchesLoginSecureRules() throws Exception {
        assertSameSecurePolicy(false, null, false);
        assertSameSecurePolicy(true, null, true);
        assertSameSecurePolicy(false, "https", true);
        assertSameSecurePolicy(false, "http", false);
    }

    @Test
    void refresh_usesSameSessionCookieHelper() throws Exception {
        Session session = registerUser();
        clearInvocations(authCookieFactory);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰 갱신 성공"))
                .andReturn();

        assertSessionCookieFlags(authSetCookies(result), false);
        verify(authCookieFactory, times(1)).sessionCookies(any(HttpServletRequest.class), anyString(), anyString());
    }

    @Test
    void logout_expiresCookiesWithSameHelperAndSecureRule() throws Exception {
        assertLogoutCookies(false, null, false);
        assertLogoutCookies(true, null, true);
        assertLogoutCookies(false, "https", true);
        assertLogoutCookies(false, "http", false);
    }

    private void assertSameSecurePolicy(boolean https, String forwardedProto, boolean expectSecure) throws Exception {
        String username = nextUsername();
        clearInvocations(authCookieFactory);
        MvcResult registered = mockMvc.perform(json(post("/api/auth/register"), registerJson(username), https, forwardedProto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입 및 로그인 성공"))
                .andReturn();
        verify(authCookieFactory, times(1)).sessionCookies(any(HttpServletRequest.class), anyString(), anyString());

        MvcResult loggedIn = mockMvc.perform(json(post("/api/auth/login"), loginJson(username), https, forwardedProto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andReturn();
        verify(authCookieFactory, times(2)).sessionCookies(any(HttpServletRequest.class), anyString(), anyString());

        List<String> registerCookies = authSetCookies(registered);
        List<String> loginCookies = authSetCookies(loggedIn);
        assertSessionCookieFlags(registerCookies, expectSecure);
        assertSessionCookieFlags(loginCookies, expectSecure);
        assertEquals(maxAge(registerCookies, AuthCookieFactory.ACCESS_TOKEN), maxAge(loginCookies, AuthCookieFactory.ACCESS_TOKEN));
        assertEquals(maxAge(registerCookies, AuthCookieFactory.REFRESH_TOKEN), maxAge(loginCookies, AuthCookieFactory.REFRESH_TOKEN));
    }

    private void assertLogoutCookies(boolean https, String forwardedProto, boolean expectSecure) throws Exception {
        clearInvocations(authCookieFactory);
        MvcResult result = mockMvc.perform(applyTransport(post("/api/auth/logout"), https, forwardedProto))
                .andExpect(status().isOk())
                .andReturn();
        verify(authCookieFactory, times(1)).expiredSessionCookies(any(HttpServletRequest.class));
        assertExpiredAuthCookies(authSetCookies(result), expectSecure);
    }

    private Session registerUser() throws Exception {
        String username = nextUsername();
        MvcResult result = mockMvc.perform(json(post("/api/auth/register"), registerJson(username), false, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입 및 로그인 성공"))
                .andReturn();
        Session session = sessionFrom(username, result);
        assertEquals(session.accessToken(), tokenService.getAccessToken(username));
        assertEquals(session.refreshToken(), tokenService.getRefreshToken(username));
        return session;
    }

    private ResultActions changePassword(Session session, String currentPassword, String newPassword,
                                         boolean https, String forwardedProto) throws Exception {
        MockHttpServletRequestBuilder builder = post("/members/profile/change-password")
                .cookie(session.accessCookie(), session.refreshCookie())
                .with(csrf())
                .param("currentPassword", currentPassword)
                .param("newPassword", newPassword)
                .param("confirmPassword", newPassword);
        return mockMvc.perform(applyTransport(builder, https, forwardedProto));
    }

    private ResultActions resetPassword(Session session, String token, boolean https, String forwardedProto) throws Exception {
        MockHttpServletRequestBuilder builder = post("/members/reset-password")
                .cookie(session.accessCookie(), session.refreshCookie())
                .with(csrf())
                .param("token", token)
                .param("password", NEW_PASSWORD)
                .param("passwordConfirm", NEW_PASSWORD);
        return mockMvc.perform(applyTransport(builder, https, forwardedProto));
    }

    private void assignResetToken(String username, String token) {
        Member member = memberRepository.findByUsername(username).orElseThrow();
        member.setResetToken(token);
        member.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        memberRepository.saveAndFlush(member);
    }

    private String passwordOf(String username) {
        return memberRepository.findByUsername(username).orElseThrow().getPassword();
    }

    private String nextUsername() {
        return "user" + SEQUENCE.incrementAndGet();
    }

    private static ResultMatcher redirectsTo(String path) {
        return result -> {
            String redirected = result.getResponse().getRedirectedUrl();
            assertTrue(redirected != null, "expected redirect to " + path);
            String withoutQuery = redirected;
            int query = withoutQuery.indexOf('?');
            if (query >= 0) {
                withoutQuery = withoutQuery.substring(0, query);
            }
            assertTrue(withoutQuery.equals(path) || withoutQuery.endsWith(path), redirected);
        };
    }

    private static String registerJson(String username) {
        return """
                {"username":"%s","email":"%s","password":"%s","passwordConfirm":"%s","country":"KR","nickname":"%s"}
                """.formatted(username, username + "@test.com", PASSWORD, PASSWORD, username);
    }

    private static String loginJson(String username) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, PASSWORD);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body,
                                                       boolean https, String forwardedProto) {
        return applyTransport(builder, https, forwardedProto)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder applyTransport(MockHttpServletRequestBuilder builder,
                                                                 boolean https, String forwardedProto) {
        if (https) {
            builder.secure(true);
        }
        if (forwardedProto != null) {
            builder.header("X-Forwarded-Proto", forwardedProto);
        }
        return builder;
    }

    private static Session sessionFrom(String username, MvcResult result) {
        List<String> cookies = authSetCookies(result);
        return new Session(
                username,
                cookieValue(header(cookies, AuthCookieFactory.ACCESS_TOKEN)),
                cookieValue(header(cookies, AuthCookieFactory.REFRESH_TOKEN)));
    }

    private static List<String> authSetCookies(MvcResult result) {
        return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(AuthCookieFactory.ACCESS_TOKEN + "=")
                        || header.startsWith(AuthCookieFactory.REFRESH_TOKEN + "="))
                .toList();
    }

    private static String header(List<String> cookies, String name) {
        return cookies.stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing cookie " + name + " in " + cookies));
    }

    private static String cookieValue(String setCookie) {
        String pair = setCookie.split(";", 2)[0];
        return pair.substring(pair.indexOf('=') + 1);
    }

    private static String maxAge(List<String> cookies, String name) {
        String cookie = header(cookies, name);
        int start = cookie.indexOf("Max-Age=");
        assertTrue(start >= 0, cookie);
        int end = cookie.indexOf(';', start);
        return end < 0 ? cookie.substring(start) : cookie.substring(start, end);
    }

    private static void assertSessionCookieFlags(List<String> cookies, boolean secure) {
        assertEquals(2, cookies.size(), cookies.toString());
        for (String cookie : cookies) {
            assertTrue(cookie.contains("; HttpOnly"), cookie);
            assertTrue(cookie.contains("; SameSite=Lax"), cookie);
            assertFalse(cookie.contains("Max-Age=0"), cookie);
            if (secure) {
                assertTrue(cookie.contains("; Secure"), cookie);
            } else {
                assertFalse(cookie.contains("Secure"), cookie);
            }
        }
    }

    private static void assertExpiredAuthCookies(List<String> cookies, boolean secure) {
        assertEquals(2, cookies.size(), cookies.toString());
        for (String cookie : cookies) {
            assertTrue(cookie.contains("Max-Age=0"), cookie);
            assertTrue(cookie.contains("; HttpOnly"), cookie);
            assertTrue(cookie.contains("; SameSite=Lax"), cookie);
            assertTrue(cookie.startsWith(AuthCookieFactory.ACCESS_TOKEN + "=;")
                    || cookie.startsWith(AuthCookieFactory.REFRESH_TOKEN + "=;"), cookie);
            if (secure) {
                assertTrue(cookie.contains("; Secure"), cookie);
            } else {
                assertFalse(cookie.contains("Secure"), cookie);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean frameworkFilterRegistered() {
        return applicationContext.getBeansOfType(FilterRegistrationBean.class).values().stream()
                .anyMatch(registration -> registration.getFilter() instanceof ForwardedHeaderFilter);
    }

    private record Session(String username, String accessToken, String refreshToken) {
        private Cookie accessCookie() {
            return new Cookie(AuthCookieFactory.ACCESS_TOKEN, accessToken);
        }

        private Cookie refreshCookie() {
            return new Cookie(AuthCookieFactory.REFRESH_TOKEN, refreshToken);
        }
    }

    @TestConfiguration
    static class ForwardedHeadersMockMvcConfig {

        @Bean
        MockMvcBuilderCustomizer forwardedHeaderFilterCustomizer(ListableBeanFactory beanFactory) {
            return new ForwardedHeaderMockMvcCustomizer(beanFactory);
        }
    }

    static final class ForwardedHeaderMockMvcCustomizer implements MockMvcBuilderCustomizer, Ordered {

        private final ListableBeanFactory beanFactory;

        private ForwardedHeaderMockMvcCustomizer(ListableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public void customize(ConfigurableMockMvcBuilder<?> builder) {
            for (FilterRegistrationBean registration : beanFactory.getBeansOfType(FilterRegistrationBean.class).values()) {
                if (registration.getFilter() instanceof ForwardedHeaderFilter filter) {
                    builder.addFilters(filter);
                }
            }
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
