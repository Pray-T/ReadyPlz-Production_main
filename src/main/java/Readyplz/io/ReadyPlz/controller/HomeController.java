package Readyplz.io.ReadyPlz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping(value = {"/", "/home"})
    public String home(Model model) {
        // JWT 기반 인증은 클라이언트 측에서 처리하므로 서버에서는 기본 정보만 전달
        return "home";
    }
} 