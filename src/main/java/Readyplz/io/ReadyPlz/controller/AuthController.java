package Readyplz.io.ReadyPlz.controller;

import Readyplz.io.ReadyPlz.domain.Member;
import Readyplz.io.ReadyPlz.domain.Role;
import Readyplz.io.ReadyPlz.dto.MemberForm;
import Readyplz.io.ReadyPlz.repository.RoleRepository;
import Readyplz.io.ReadyPlz.service.CustomUserDetailsService;
import Readyplz.io.ReadyPlz.service.MemberService;
import Readyplz.io.ReadyPlz.service.TokenService;
import Readyplz.io.ReadyPlz.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final long ACCESS_TOKEN_EXPIRATION_MS = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 43200000L; // 12시간

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenService tokenService;
    private final MemberService memberService;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @PostMapping("/login") //loginRequest는 클라이언트가 보내는 데이터
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest, HttpServletRequest request, HttpServletResponse response) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            // log.info("로그인 시도: {}", username);

            // 인증 시도
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // JWT 토큰 생성
            String accessToken = jwtTokenUtil.generateAccessToken(userDetails);
            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

            // Redis에 토큰 저장
            tokenService.saveAccessToken(username, accessToken, ACCESS_TOKEN_EXPIRATION_MS);
            tokenService.saveRefreshToken(username, refreshToken, REFRESH_TOKEN_EXPIRATION_MS);

            boolean secure = request.isSecure();
            ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(ACCESS_TOKEN_EXPIRATION_MS / 1000)
                .build();
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(REFRESH_TOKEN_EXPIRATION_MS / 1000)
                .build();

            Map<String, Object> body = new HashMap<>();
            body.put("accessToken", accessToken);
            body.put("username", username);
            body.put("message", "로그인 성공");

            // log.info("로그인 성공: {}", username);
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                .body(body);

        } catch (BadCredentialsException e) {
            log.warn("로그인 실패 (자격 증명 오류): {}", loginRequest.get("username"));
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage(), e);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "로그인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody MemberForm memberForm) {
        try {
            log.info("회원가입 시도: {}", memberForm.getUsername());

            // 비밀번호 확인 검증
            if (memberForm.getPassword() == null || memberForm.getPasswordConfirm() == null ||
                !memberForm.getPassword().equals(memberForm.getPasswordConfirm())) {
                return ResponseEntity.badRequest().body(Map.of("message", "비밀번호가 일치하지 않습니다."));
            }

            // ROLE_USER 권한 자동 부여
            Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);

            Member member = Member.builder()
                .username(memberForm.getUsername())
                .email(memberForm.getEmail())
                .password(passwordEncoder.encode(memberForm.getPassword()))
                .nickname(memberForm.getNickname())
                .country(memberForm.getCountry())
                .roles(roles)
                .build();

            // 회원가입 처리 (별도 트랜잭션)
            Long memberId = memberService.register(member);

            log.info("회원가입 성공: {} (ID: {})", memberForm.getUsername(), memberId);

            // 회원가입 후 자동 로그인 시도 (별도 트랜잭션)
            return performAutoLogin(memberForm.getUsername(), memberForm.getPassword(), null);

        } catch (IllegalStateException e) {
            log.error("회원가입 실패 (중복 회원): {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "회원가입 중 오류가 발생했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }

   

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            String refreshToken = null;
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("refreshToken".equals(c.getName())) {
                        refreshToken = c.getValue();
                        break;
                    }
                }
            }

            if (refreshToken == null || refreshToken.isBlank()) {
                Map<String, Object> body = new HashMap<>();
                body.put("message", "리프레시 토큰이 없습니다");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            // 리프레시 토큰 검증
            if (!jwtTokenUtil.isRefreshToken(refreshToken)) {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "유효하지 않은 리프레시 토큰입니다");
                return ResponseEntity.badRequest().body(errorBody);
            }

            String username = jwtTokenUtil.extractUsername(refreshToken);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

            if (!jwtTokenUtil.validateToken(refreshToken, userDetails)) {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "만료된 리프레시 토큰입니다");
                return ResponseEntity.badRequest().body(errorBody);
            }

            // Redis에 저장된 리프레시 토큰과 비교
            String storedRefreshToken = tokenService.getRefreshToken(username);
            if (!refreshToken.equals(storedRefreshToken)) {
                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("message", "재사용 혹은 위조된된 리프레시 토큰입니다");
                return ResponseEntity.badRequest().body(errorBody);
            }

            // 새로운 액세스 토큰과 리프레시 토큰 생성 (Rotation)
            String newAccessToken = jwtTokenUtil.generateAccessToken(userDetails);
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

            // 새로운 토큰들을 Redis에 저장
            tokenService.saveAccessToken(username, newAccessToken, ACCESS_TOKEN_EXPIRATION_MS); 
            tokenService.saveRefreshToken(username, newRefreshToken, REFRESH_TOKEN_EXPIRATION_MS); 

            boolean secure = request.isSecure();
            ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(ACCESS_TOKEN_EXPIRATION_MS / 1000)
                .build();
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(REFRESH_TOKEN_EXPIRATION_MS / 1000)
                .build();

            Map<String, Object> body = new HashMap<>();
            body.put("accessToken", newAccessToken);
            body.put("username", username);
            body.put("message", "토큰 갱신 성공");

            log.info("토큰 갱신 성공: {}", username);
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                .body(body);

        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("message", "토큰 갱신 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) Map<String, String> logoutRequest, HttpServletRequest request) {
        try {
            // 1) 액세스 토큰 추출: 바디 -> Authorization 헤더 -> 쿠키 순으로
            String accessToken = null;
            if (logoutRequest != null) {
                accessToken = logoutRequest.get("accessToken");
            }
            if (accessToken == null) {
                final String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    accessToken = authHeader.substring(7);
                }
            }
            if (accessToken == null) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        if ("accessToken".equals(c.getName())) {
                            accessToken = c.getValue();
                            break;
                        }
                    }
                }
            }

            // 2) 사용자명 결정: accessToken 우선, 없으면 refreshToken 쿠키에서 추출 시도
            String username = null;
            if (accessToken != null && !accessToken.isBlank()) {
                try {
                    username = jwtTokenUtil.extractUsername(accessToken);
                } catch (Exception ignored) {}
            }
            if (username == null) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        if ("refreshToken".equals(c.getName())) {
                            String refresh = c.getValue();
                            try {
                                username = jwtTokenUtil.extractUsername(refresh);
                            } catch (Exception ignored) {}
                            break;
                        }
                    }
                }
            }

            // 토큰을 블랙리스트에 추가
            if (accessToken != null && !accessToken.isBlank()) {
                tokenService.addToBlacklist(accessToken, 3600000L);
            }

            // 사용자의 모든 토큰 삭제
            if (username != null && !username.isBlank()) {
                tokenService.deleteUserTokens(username);
            }

            boolean secure = request.isSecure();
            ResponseCookie deleteAccessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
            ResponseCookie deleteRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

            Map<String, String> body = new HashMap<>();
            body.put("message", "로그아웃 성공");

            log.info("로그아웃 성공: {}", username != null ? username : "unknown-user");
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString(), deleteRefreshCookie.toString())
                .body(body);

        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("message", "로그아웃 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, String>> validateToken(HttpServletRequest request) {
        try {
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
                return ResponseEntity.status(401).body(Map.of("message", "인증되지 않았습니다."));
            }

            String username = authentication.getName();
            return ResponseEntity.ok(Map.of("message", "토큰이 유효합니다.", "username", username));

        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("message", "토큰 검증 실패: " + e.getMessage()));
        }
    }

     private ResponseEntity<Map<String, Object>> performAutoLogin(String username, String password, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtTokenUtil.generateAccessToken(userDetails);
            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

            tokenService.saveAccessToken(username, accessToken, ACCESS_TOKEN_EXPIRATION_MS);
            tokenService.saveRefreshToken(username, refreshToken, REFRESH_TOKEN_EXPIRATION_MS);

            boolean secure = request != null && request.isSecure();
            ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(ACCESS_TOKEN_EXPIRATION_MS / 1000)
                .build();
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(REFRESH_TOKEN_EXPIRATION_MS / 1000)
                .build();

            Map<String, Object> body = new HashMap<>();
            body.put("accessToken", accessToken);
            body.put("username", username);
            body.put("message", "회원가입 및 로그인 성공");

            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                .body(body);

        } catch (Exception loginException) {
            log.warn("회원가입 성공했으나 자동 로그인 실패: {}", loginException.getMessage());

            // 회원가입만 성공한 경우
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("username", username);
            errorBody.put("message", "회원가입 성공. 로그인을 시도해주세요.");
            return ResponseEntity.ok(errorBody);
        }
    }
} 