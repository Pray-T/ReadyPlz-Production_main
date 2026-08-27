package io.readyplz.readyplz.config;

import io.readyplz.readyplz.security.CsrfCookieFilter;
import io.readyplz.readyplz.security.JwtAuthenticationFilter;
import io.readyplz.readyplz.security.NonClearingCookieCsrfTokenRepository;
import io.readyplz.readyplz.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // SSR 폼(_csrf)과 JS(X-XSRF-TOKEN 쿠키 복사)가 동일한 raw 토큰을 쓰도록 XOR 핸들러는 사용하지 않음
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName("_csrf");

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(new NonClearingCookieCsrfTokenRepository())
                .csrfTokenRequestHandler(csrfRequestHandler)
                .ignoringRequestMatchers("/api/**"))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // 공개 접근 가능한 경로 (정적 리소스 포함)
                .requestMatchers("/", "/home", "/members/register", "/members/loginForm", 
                               "/members/reset-request", "/members/reset-password",
                               "/api/auth/**", "/health",
                               "/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                
                // 관리자 전용 경로
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // 인증된 사용자만 접근 가능한 경로
                .requestMatchers("/games/**", "/members/profile/**", "/messages/**").authenticated()
                
                // 기타 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint()))

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            // CSRF 검사 전에 JWT를 적용해야, CSRF 실패 시 로그인 리다이렉트(로그아웃처럼 보임)를 피함
            .addFilterBefore(jwtAuthFilter, CsrfFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"unauthorized\"}");
            } else {
                response.sendRedirect("/members/loginForm");
            }
        };
    }

    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() { 
        CorsConfiguration configuration = new CorsConfiguration(); 
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); 
        configuration.setAllowedHeaders(Arrays.asList("*")); //모든 헤더 허용
        configuration.setAllowCredentials(true); //클라이언트가 쿠키를 포함한 요청을 보낼 수 있도록 허용
        configuration.setExposedHeaders(Arrays.asList("Authorization")); //응답 헤더에 포함될 헤더 목록
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); //CORS Configuration 소스 생성
        source.registerCorsConfiguration("/**", configuration); //모든 경로에 대해 CORS 설정 적용
        return source; 
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    //시큐리티를 통해서 비밀번호를 암호화 시켜서 저장해야 하기에 필요함.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
