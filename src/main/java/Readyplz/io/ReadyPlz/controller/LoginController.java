package Readyplz.io.ReadyPlz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
public class LoginController {

    private final MessageSource messageSource;

    @GetMapping("/loginForm")
    public String loginPage(
                           @RequestParam(value = "msgCode", required = false) String msgCode,
                           @RequestParam(value = "errorCode", required = false) String errorCode,
                           Model model, Locale locale) {
        
        if (msgCode != null) {
            String message = messageSource.getMessage(msgCode, null, locale);
            model.addAttribute("msg", message);
        }

        if (errorCode != null) {
            String error = messageSource.getMessage(errorCode, null, locale);
            model.addAttribute("error", error);
        }
        
        return "members/loginForm";
    }
} 