package Readyplz.io.ReadyPlz.controller;

import Readyplz.io.ReadyPlz.domain.Game;
import Readyplz.io.ReadyPlz.domain.Member;
import Readyplz.io.ReadyPlz.domain.MemberGame;
import Readyplz.io.ReadyPlz.service.MemberGameService;
import Readyplz.io.ReadyPlz.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import Readyplz.io.ReadyPlz.dto.request.UpdateNicknameRequest;
import Readyplz.io.ReadyPlz.dto.request.ChangePasswordRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import Readyplz.io.ReadyPlz.dto.request.DeleteAccountRequest;
import Readyplz.io.ReadyPlz.service.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final MemberService memberService;
    private final MemberGameService memberGameService;
    private final TokenService tokenService;

    @GetMapping("/members/profile")
    public String profilePage(Authentication auth, Model model) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);
        
        // 사용자가 가진 게임 목록 조회
        Set<MemberGame> memberGames = memberGameService.findByMember(member);
        List<Game> userGames = memberGames.stream()
                .map(MemberGame::getGame)
                .collect(Collectors.toList());
        
        model.addAttribute("member", member);
        model.addAttribute("userGames", userGames);
        return "members/profile";
    }

    @PostMapping("/members/profile/delete-account")
    public String deleteAccount(Authentication auth,
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

            // 4) 성공 시에만 토큰 무효화 및 쿠키 삭제
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
    public String confirmNicknameUpdate(@Valid @ModelAttribute UpdateNicknameRequest request,
                                        Authentication auth, Model model, RedirectAttributes redirectAttributes) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        // DTO 기반 유효성 검증 결과는 BindingResult로 처리 가능하나, 간단히 Flash 메시지로 표시

        model.addAttribute("member", member);
        model.addAttribute("newNickname", request.getNickname().trim());
        model.addAttribute("currentNickname", member.getNickname());
        
        return "members/confirm-nickname";
    }

    @PostMapping("/members/profile/update-nickname-confirmed")
    public String updateNicknameConfirmed(@Valid @ModelAttribute UpdateNicknameRequest request,
                                          Authentication auth,
                                          RedirectAttributes redirectAttributes) {

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
    public String changePassword(Authentication auth,
                               @Valid @ModelAttribute ChangePasswordRequest request,
                               RedirectAttributes redirectAttributes) {

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