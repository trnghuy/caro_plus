package com.example.caro_plus.controller;

import com.example.caro_plus.model.User;
import com.example.caro_plus.service.UserService;
// import jakarta.servlet.http.Cookie;
// import jakarta.servlet.http.HttpServletResponse;
// import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // @PostMapping("/login")
    // public String login(@RequestParam String username,
    //         @RequestParam String password,
    //         @RequestParam(value = "remember", required = false) String remember,
    //         HttpSession session,
    //         HttpServletResponse response,
    //         RedirectAttributes ra) {
    //     try {
    //         User user = userService.login(username, password);
    //         if (user != null) {
    //             session.setAttribute("user", user);

    //             // Thêm Ghi nhớ đăng nhập (Remember Me)
    //             Cookie cookie = new Cookie("rememberedUser", username);
    //             cookie.setPath("/");
    //             if (remember != null) {
    //                 cookie.setMaxAge(7 * 24 * 60 * 60); // 7 ngày
    //             } else {
    //                 cookie.setMaxAge(0); // Xóa cookie nếu không tích
    //             }
    //             response.addCookie(cookie);

    //             return "redirect:/home"; // Đăng nhập xong vào game
    //         }
    //     } catch (Exception e) {
    //         ra.addFlashAttribute("error", "Lỗi: Tài khoản bị trùng lặp trong hệ thống!");
    //     }

    //     // Nếu thất bại: Quay về trang chủ và tự mở Modal Login
    //     ra.addFlashAttribute("error", "Tài khoản hoặc mật khẩu không chính xác!");
    //     ra.addFlashAttribute("openModal", "login");
    //     return "redirect:/";
    // }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, RedirectAttributes ra) {
        if (userService.findByUsername(user.getUsername()) != null) {
            ra.addFlashAttribute("error", "Tên đăng nhập đã tồn tại!");
            ra.addFlashAttribute("openModal", "register");
            return "redirect:/";
        }

        userService.register(user); // Hàm này của bạn đã có encode password rồi, rất chuẩn!
        ra.addFlashAttribute("message", "Đăng ký thành công! Hãy đăng nhập.");
        ra.addFlashAttribute("openModal", "login");
        return "redirect:/";
    }

    // @GetMapping("/logout")
    // public String logout(HttpSession session) {
    //     session.invalidate();
    //     return "redirect:/";
    // }
}