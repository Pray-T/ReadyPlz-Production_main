package Readyplz.io.ReadyPlz.config;

import Readyplz.io.ReadyPlz.security.JwtAuthenticationFilter;
import Readyplz.io.ReadyPlz.service.CustomUserDetailsService;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // 토큰 기반 인증이므로 CSRF 보안 비활성화
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // 공개 접근 가능한 경로 (정적 리소스 포함)
                .requestMatchers("/", "/members/register", "/members/loginForm", 
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
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
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
        configuration.setAllowedOriginPatterns(Arrays.asList("https://readyplz.com", "https://www.readyplz.com"," https://*.readyplz.com")); //프로덕션 도메인만 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); //모든 HTTP 메서드 허용
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
