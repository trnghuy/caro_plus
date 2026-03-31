package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import com.example.caro_plus.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public String register(@ModelAttribute User user, RedirectAttributes ra) {
        if (userService.findByUsername(user.getUsername()) != null) {
            ra.addFlashAttribute("error", "Tên đăng nhập đã tồn tại!");
            ra.addFlashAttribute("openModal", "register");
            return "redirect:/";
        }

        userService.register(user);
        ra.addFlashAttribute("message", "Đăng ký thành công! Hãy đăng nhập.");
        ra.addFlashAttribute("openModal", "login");
        return "redirect:/";
    }
}
