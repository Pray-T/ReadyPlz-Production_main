package io.readyplz.readyplz.controller;

import io.readyplz.readyplz.dto.CountryDTO;
import io.readyplz.readyplz.dto.MemberForm;
import io.readyplz.readyplz.dto.request.ResetPasswordRequest;
import io.readyplz.readyplz.security.AuthCookieFactory;
import io.readyplz.readyplz.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final AuthCookieFactory authCookieFactory;

    @GetMapping("/register")
    public String registerForm(Model model, Locale locale) {
        model.addAttribute("memberForm", new MemberForm());
        // 모든 국가 리스트 동적 생성
        String[] countryCodes = Locale.getISOCountries();
        List<CountryDTO> countryList = new ArrayList<>();
        for (String code : countryCodes) {
            Locale countryLocale = new Locale("", code);
            countryList.add(new CountryDTO(code, countryLocale.getDisplayCountry(locale)));
        }
        countryList.sort(Comparator.comparing(CountryDTO::getName));
        model.addAttribute("countryList", countryList);
        return "members/registerForm";
    }

    
    @GetMapping("/reset-request") 
    public String resetRequestForm() {
        return "members/reset-request";
    }
    
    
    @PostMapping("/reset-request") 
    public String handleResetRequest(@RequestParam("email") String email, RedirectAttributes redirectAttributes) { 
        memberService.createPasswordResetToken(email); 
        redirectAttributes.addFlashAttribute("msg", "입력하신 이메일로 비밀번호 재설정 메일이 발송되었습니다."); 
        return "redirect:/members/loginForm"; 
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam("token") String token, Model model) {
        if (!memberService.isResetTokenValid(token)) {
            model.addAttribute("error", "유효하지 않거나 만료된 토큰입니다.");
            return "members/reset-password";
        }
        model.addAttribute("token", token);
        return "members/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@Valid @ModelAttribute ResetPasswordRequest request,
                                      BindingResult bindingResult,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse httpResponse,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", validationErrorMessage(bindingResult));
            redirectAttributes.addAttribute("token", request.getToken());
            return "redirect:/members/reset-password";
        }

        try {
            memberService.resetPassword(request.getToken(), request.getPassword());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("token", request.getToken());
            return "redirect:/members/reset-password";
        } catch (RuntimeException e) {
            log.error("비밀번호 재설정 후 세션 종료에 실패했습니다.", e);
            redirectAttributes.addFlashAttribute("error", "비밀번호 변경에 실패했습니다. 다시 시도해주세요.");
            redirectAttributes.addAttribute("token", request.getToken());
            return "redirect:/members/reset-password";
        }

        authCookieFactory.expireSession(httpRequest, httpResponse);
        redirectAttributes.addFlashAttribute("msg", "비밀번호가 변경되었습니다. 다시 로그인해 주세요.");
        return "redirect:/members/loginForm";
    }

    private static String validationErrorMessage(BindingResult bindingResult) {
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
            return fieldError.getDefaultMessage();
        }
        ObjectError globalError = bindingResult.getGlobalError();
        if (globalError != null && globalError.getDefaultMessage() != null && !globalError.getDefaultMessage().isBlank()) {
            return globalError.getDefaultMessage();
        }
        return "입력값을 확인해주세요.";
    }
} 