package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import com.example.caro_plus.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

@GetMapping("/login")
public String showLogin() {
    return "welcome"; // dùng lại trang chính có modal
}

@PostMapping("/login")
public String login(@RequestParam String username,
                    @RequestParam String password,
                    // ... các params khác
                    HttpSession session,
                    RedirectAttributes ra) {
    try {
        User user = userService.login(username, password);
        if (user != null) {
            session.setAttribute("user", user);
            return "redirect:/"; // Quay lại trang chủ để HomeController xử lý hiển thị account
        }
    } catch (Exception e) {
        ra.addFlashAttribute("error", "Lỗi hệ thống!");
    }
    ra.addFlashAttribute("error", "Tài khoản hoặc mật khẩu không chính xác!");
    ra.addFlashAttribute("openModal", "login"); 
    return "redirect:/";
}
    @PostMapping("/register")
    public String register(@ModelAttribute User user, RedirectAttributes ra) {
        if (userService.findByUsername(user.getUsername()) != null) {
            ra.addFlashAttribute("error", "Tên đăng nhập đã tồn tại!");
            ra.addFlashAttribute("openModal", "register");
            return "redirect:/";
        }

        userService.register(user);
        ra.addFlashAttribute("message", "Đăng ký thành công! Hãy đăng nhập.");
        ra.addFlashAttribute("openModal", "login"); // Đăng ký xong tự mở Modal Login
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}