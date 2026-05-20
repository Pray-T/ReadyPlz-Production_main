package io.readyplz.readyplz.controller;

import io.readyplz.readyplz.config.JwtProperties;
import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.readyplz.readyplz.dto.request.UpdateNicknameRequest;
import io.readyplz.readyplz.dto.request.ChangePasswordRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import io.readyplz.readyplz.dto.request.DeleteAccountRequest;
import io.readyplz.readyplz.service.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final MemberService memberService;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    private void blacklistSessionTokens(HttpServletRequest request) {
        String accessToken = null;
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("accessToken".equals(c.getName())) {
                    accessToken = c.getValue();
                } else if ("refreshToken".equals(c.getName())) {
                    refreshToken = c.getValue();
                }
            }
        }
        if (accessToken != null && !accessToken.isBlank()) {
            tokenService.addToBlacklist(accessToken, jwtProperties.getAccessTokenValidity());
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.addToBlacklist(refreshToken, jwtProperties.getRefreshTokenValidity());
        }
    }

    /** Bean Validation 오류 → 플래시 속성용 코드로 변환 (필드 오류 우선, 클래스 레벨은 글로벌 오류) */
    private static String validationFlashCode(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
            return fieldError.getDefaultMessage();
        }
        List<ObjectError> globals = bindingResult.getGlobalErrors();
        if (!globals.isEmpty()) {
            ObjectError oe = globals.get(0);
            if (oe.getDefaultMessage() != null && !oe.getDefaultMessage().isBlank()) {
                return oe.getDefaultMessage();
            }
        }
        return "validation_error";
    }

    @GetMapping("/members/profile")
    public String profilePage(Authentication auth, Model model) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        model.addAttribute("member", member);
        return "members/profile";
    }

    @PostMapping("/members/profile/delete-account")
    public String deleteAccount(
                                Authentication auth,
                                @Valid @ModelAttribute DeleteAccountRequest request,
                                BindingResult bindingResult, 
                                HttpServletRequest httpRequest, 
                                HttpServletResponse httpResponse, 
                                RedirectAttributes redirectAttributes) {

        String username = auth.getName(); 

        // 1) 폼 유효성 검증 실패 시: 토큰 조작 없이 프로필로 리다이렉트
        if (bindingResult.hasErrors()) {
            // 비어있거나 규칙 불일치: 공통 오류 키 사용
            boolean emailError = bindingResult.getFieldErrors().stream().anyMatch(fe -> "email".equals(fe.getField())); 
            redirectAttributes.addFlashAttribute("error", emailError ? "email_input_error" : "password_input_error");
            return "redirect:/members/profile";
        }

        try {
            Member member = memberService.findByUsername(username);
            // 2) 이메일 확인 실패 시: 토큰 조작 금지, 프로필로 복귀 (입력값 공백 제거 후 비교)
            String reqEmail = request.getEmail() == null ? "" : request.getEmail().trim();
            if (!member.getEmail().equalsIgnoreCase(reqEmail)) {
                redirectAttributes.addFlashAttribute("error", "email_input_error");
                return "redirect:/members/profile";
            }

            // 3) 비밀번호 확인 및 삭제 수행 (내부에서 비밀번호 불일치 시 예외 발생)
            memberService.deleteOwnAccount(username, request.getPassword());

            // 4) 성공 시에만 토큰 무효화 및 쿠키 삭제 (로그아웃과 동일: 블랙리스트 후 Redis 삭제)
            blacklistSessionTokens(httpRequest);
            tokenService.deleteUserTokens(username);

            boolean secure = httpRequest.isSecure();
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
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString());
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());

            redirectAttributes.addFlashAttribute("success", "account_deleted");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            String reason = e.getMessage();
            if (reason == null || reason.isBlank()) reason = "password_input_error";
            redirectAttributes.addFlashAttribute("error", reason);
            return "redirect:/members/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "delete_failed");
            return "redirect:/members/profile";
        }
    }

    @PostMapping("/members/profile/confirm-nickname")
    public String confirmNicknameUpdate(
                                        @Valid @ModelAttribute UpdateNicknameRequest request,
                                        BindingResult bindingResult,
                                        Authentication auth,
                                        RedirectAttributes redirectAttributes,
                                        Model model) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", validationFlashCode(bindingResult));
            return "redirect:/members/profile";
        }

        String username = auth.getName();
        Member member = memberService.findByUsername(username);


        model.addAttribute("member", member);
        model.addAttribute("newNickname", request.getNickname().trim());
        model.addAttribute("currentNickname", member.getNickname());
        
        return "members/confirm-nickname";
    }

    @PostMapping("/members/profile/update-nickname-confirmed")
    public String updateNicknameConfirmed(@Valid @ModelAttribute UpdateNicknameRequest request,
                                          BindingResult bindingResult,
                                          Authentication auth,
                                          RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", validationFlashCode(bindingResult));
            return "redirect:/members/profile";
        }

        String username = auth.getName();
        try {
            memberService.updateNickname(username, request.getNickname());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "duplicate_nickname");
            return "redirect:/members/profile";
        }

        redirectAttributes.addFlashAttribute("success", "nickname_updated");
        return "redirect:/members/profile";
    }

    @PostMapping("/members/profile/change-password")
    public String changePassword(@Valid @ModelAttribute ChangePasswordRequest request,
                                BindingResult bindingResult,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", validationFlashCode(bindingResult));
            return "redirect:/members/profile";
        }

        String username = auth.getName();
        try {
            memberService.changePassword(username, request.getCurrentPassword(), request.getNewPassword(), request.getConfirmPassword());
            redirectAttributes.addFlashAttribute("success", "password_updated");
            return "redirect:/members/profile";
        } catch (IllegalArgumentException e) {
            String reason = e.getMessage();
            if (reason == null || reason.isBlank()) reason = "password_input_error";
            redirectAttributes.addFlashAttribute("error", reason);
            return "redirect:/members/profile";
        }
    }
} 