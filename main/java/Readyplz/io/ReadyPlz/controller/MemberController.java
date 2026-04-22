package Readyplz.io.ReadyPlz.controller;

import Readyplz.io.ReadyPlz.dto.CountryDTO;
import Readyplz.io.ReadyPlz.dto.MemberForm;
import Readyplz.io.ReadyPlz.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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
    public String handleResetPassword(@RequestParam("token") String token, @RequestParam("password") String password, @RequestParam("passwordConfirm") String passwordConfirm, RedirectAttributes redirectAttributes) {
        if (!password.equals(passwordConfirm)) {
            redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
            redirectAttributes.addAttribute("token", token);
            return "redirect:/members/reset-password";
        }

        try {
            memberService.resetPassword(token, password);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("token", token);
            return "redirect:/members/reset-password";
        }

        redirectAttributes.addFlashAttribute("msg", "비밀번호가 성공적으로 변경되었습니다.");
        return "redirect:/members/loginForm";
    }
} 