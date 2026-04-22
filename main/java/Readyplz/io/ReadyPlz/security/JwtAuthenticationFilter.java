package Readyplz.io.ReadyPlz.security;

import Readyplz.io.ReadyPlz.util.JwtTokenUtil;
import Readyplz.io.ReadyPlz.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;


    @Override 
    protected void doFilterInternal( 
            @NonNull HttpServletRequest request, 
            @NonNull HttpServletResponse response, 
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization"); //Request의 Header에서 Authorization 헤더 추출
        String jwt = null;
        String username = null;
        
        // 디버깅 로그(필요시 활성화)
        // log.debug("=== JWT 인증 필터 디버깅 ===");
        // log.debug("요청 URL: {}", request.getRequestURI());
        // log.debug("Authorization 헤더: {}", authHeader);
        
        // 1) 쿠키에서 액세스 토큰 우선 추출 (브라우저 네비게이션 대응)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("accessToken".equals(c.getName())) {
                    jwt = c.getValue();
                    break;
                }
            }
        }

        // 2) Authorization 헤더에서 JWT 토큰 보조 추출
        if (jwt == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); //Bearer 토큰 제거
        }
        // 3) URL 쿼리/폼 데이터에서 토큰은 허용하지 않음 (보안상 금지)
        
        if (jwt == null) {
            // log.debug("JWT 토큰이 없음 - 인증 없이 진행");
            filterChain.doFilter(request, response); //현재 필터에서 처리가 끊겨도 다음 필터로 요청 전달
            return;
        }
        
        // 블랙리스트 토큰 여부 확인 (로그아웃 등으로 무효화된 토큰)
        try {
            if (tokenService.isBlacklisted(jwt)) {
                // log.warn("블랙리스트 토큰 접근 차단");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            // log.warn("블랙리스트 확인 중 오류: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            username = jwtTokenUtil.extractUsername(jwt);
            // log.debug("JWT에서 사용자명 추출: {}", username);
        } catch (Exception e) {
            // log.warn("JWT 토큰에서 사용자명 추출 실패: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }
        // 5. 사용자 인증 처리 
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {  
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username); //사용자 상세 정보 로드
                // log.debug("사용자 상세 정보 로드: {}", username);
                
                if (jwtTokenUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()); //인증 토큰 생성
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); 
                    SecurityContextHolder.getContext().setAuthentication(authToken);  
                    // log.debug("JWT 인증 성공: {} (권한: {})", username, userDetails.getAuthorities());
                } else {
                    // log.warn("JWT 토큰 검증 실패: {}", username);
                    SecurityContextHolder.clearContext(); //
                }
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                // log.warn("JWT 토큰의 사용자가 DB에 존재하지 않음: {}", username);
                SecurityContextHolder.clearContext();
            } catch (Exception e) {
                // log.warn("JWT 인증 처리 중 오류: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        } else if (username != null) {
            // log.debug("이미 인증된 사용자: {}", username);
        }
        
        // log.debug("=== JWT 인증 필터 완료 ===");
        filterChain.doFilter(request, response);
    }
} 