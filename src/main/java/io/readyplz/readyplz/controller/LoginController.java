package io.readyplz.readyplz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
public class LoginController {

    private static final String UNKNOWN_MSG_TEXT = "메시지를 표시할 수 없습니다.";
    private static final String UNKNOWN_ERROR_TEXT = "오류가 발생했습니다. 다시 시도해주세요.";

    private final MessageSource messageSource;

    @GetMapping("/loginForm")
    public String loginPage(
                           @RequestParam(value = "msgCode", required = false) String msgCode,
                           @RequestParam(value = "errorCode", required = false) String errorCode,
                           Model model, Locale locale) {

        Locale resolvedLocale = Objects.requireNonNull(
                locale != null ? locale : Locale.getDefault());

        if (msgCode != null && !msgCode.isBlank()) {
            model.addAttribute("msg", resolveMessage(msgCode, UNKNOWN_MSG_TEXT, resolvedLocale));
        }

        if (errorCode != null && !errorCode.isBlank()) {
            model.addAttribute("error", resolveMessage(errorCode, UNKNOWN_ERROR_TEXT, resolvedLocale));
        }
        
        return "members/loginForm";
    }

    private String resolveMessage(String code, String fallback, Locale locale) {
        return messageSource.getMessage(code, null, fallback, locale);
    }
} 
