package io.readyplz.readyplz.exception;

import io.readyplz.readyplz.controller.GameController;
import io.readyplz.readyplz.controller.HomeController;
import io.readyplz.readyplz.controller.LoginController;
import io.readyplz.readyplz.controller.MemberController;
import io.readyplz.readyplz.controller.MessageController;
import io.readyplz.readyplz.controller.ProfileController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = {
        ProfileController.class,
        MemberController.class,
        GameController.class,
        MessageController.class,
        HomeController.class,
        LoginController.class
})
public class MvcExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentForMvc(IllegalArgumentException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error",
                ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다.");
        return "redirect:/members/profile";
    }
}
