package com.example.caro_plus.controller;

import com.example.caro_plus.model.ChatMessage;
import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.AdminService;
import com.example.caro_plus.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @GetMapping("/admin")
    public String adminDashboard(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                 Model model) {
        populateAdminShell(model, customUserDetails, "dashboard", "Tổng quan hệ thống");
        model.addAttribute("overview", adminService.getOverviewMetrics());
        model.addAttribute("recentUsers", adminService.getRecentUsers());
        model.addAttribute("recentRooms", adminService.getRecentRooms());
        model.addAttribute("recentGames", adminService.getRecentGames());
        model.addAttribute("recentMessages", adminService.getRecentMessages());
        model.addAttribute("topPointUsers", adminService.getTopPointUsers());
        model.addAttribute("topStarUsers", adminService.getTopStarUsers());
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String adminUsers(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                             @RequestParam(defaultValue = "") String userQ,
                             @RequestParam(defaultValue = "") String userRole,
                             @RequestParam(defaultValue = "recent") String userSort,
                             @RequestParam(defaultValue = "0") int userPage,
                             Model model) {
        populateAdminShell(model, customUserDetails, "users", "Quản lý người dùng");
        Page<User> users = adminService.getUsersPage(userQ, userRole, userSort, userPage);
        model.addAttribute("users", users);
        model.addAttribute("userQ", userQ);
        model.addAttribute("userRole", userRole);
        model.addAttribute("userSort", userSort);
        return "admin/users";
    }

    @GetMapping("/admin/rooms")
    public String adminRooms(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                             @RequestParam(defaultValue = "") String roomQ,
                             @RequestParam(defaultValue = "") String roomStatus,
                             @RequestParam(defaultValue = "0") int roomPage,
                             Model model) {
        populateAdminShell(model, customUserDetails, "rooms", "Quản lý phòng");
        Page<Room> rooms = adminService.getRoomsPage(roomQ, roomStatus, roomPage);
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomQ", roomQ);
        model.addAttribute("roomStatus", roomStatus);
        return "admin/rooms";
    }

    @GetMapping("/admin/games")
    public String adminGames(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                             @RequestParam(defaultValue = "") String gameQ,
                             @RequestParam(defaultValue = "") String gameStatus,
                             @RequestParam(defaultValue = "0") int gamePage,
                             Model model) {
        populateAdminShell(model, customUserDetails, "games", "Quản lý trận đấu");
        Page<Game> games = adminService.getGamesPage(gameQ, gameStatus, gamePage);
        model.addAttribute("games", games);
        model.addAttribute("gameQ", gameQ);
        model.addAttribute("gameStatus", gameStatus);
        return "admin/games";
    }

    @GetMapping("/admin/messages")
    public String adminMessages(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                @RequestParam(defaultValue = "") String messageQ,
                                @RequestParam(required = false) Long messageRoomId,
                                @RequestParam(defaultValue = "0") int messagePage,
                                Model model) {
        populateAdminShell(model, customUserDetails, "messages", "Quản lý chat");
        Page<ChatMessage> messages = adminService.getMessagesPage(messageQ, messageRoomId, messagePage);
        model.addAttribute("messages", messages);
        model.addAttribute("messageQ", messageQ);
        model.addAttribute("messageRoomId", messageRoomId);
        return "admin/messages";
    }

    @GetMapping("/admin/stats")
    public String adminStats(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                             Model model) {
        populateAdminShell(model, customUserDetails, "stats", "Thống kê hệ thống");
        model.addAttribute("overview", adminService.getOverviewMetrics());
        model.addAttribute("topPointUsers", adminService.getTopPointUsers());
        model.addAttribute("topStarUsers", adminService.getTopStarUsers());
        return "admin/stats";
    }

    @GetMapping("/admin/users/{id}")
    public String userDetail(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails customUserDetails,
                             Model model) {
        model.addAttribute("adminUser", userService.getPersistedUser(customUserDetails.getUser()));
        model.addAttribute("detail", adminService.getUserDetail(id));
        return "admin/user-detail";
    }

    @GetMapping("/admin/rooms/{id}")
    public String roomDetail(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails customUserDetails,
                             Model model) {
        model.addAttribute("adminUser", userService.getPersistedUser(customUserDetails.getUser()));
        model.addAttribute("detail", adminService.getRoomDetail(id));
        return "admin/room-detail";
    }

    @GetMapping("/admin/games/{id}")
    public String gameDetail(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails customUserDetails,
                             Model model) {
        model.addAttribute("adminUser", userService.getPersistedUser(customUserDetails.getUser()));
        model.addAttribute("detail", adminService.getGameDetail(id));
        return "admin/game-detail";
    }

    @PostMapping("/admin/users/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam String role,
                                 @AuthenticationPrincipal CustomUserDetails customUserDetails,
                                 RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.updateUserRole(id, role, customUserDetails.getUsername()),
                "Đã cập nhật quyền tài khoản.", "redirect:/admin/users", redirectAttributes);
    }

    @PostMapping("/admin/users/{id}/lock")
    public String lockUser(@PathVariable Long id,
                           @AuthenticationPrincipal CustomUserDetails customUserDetails,
                           RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.lockUser(id, customUserDetails.getUsername()),
                "Đã khóa tài khoản.", "redirect:/admin/users", redirectAttributes);
    }

    @PostMapping("/admin/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.unlockUser(id),
                "Đã mở khóa tài khoản.", "redirect:/admin/users", redirectAttributes);
    }

    @PostMapping("/admin/users/{id}/reset-stars")
    public String resetUserStars(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.resetUserStars(id),
                "Đã đặt lại sao về mức mặc định.", "redirect:/admin/users", redirectAttributes);
    }

    @PostMapping("/admin/users/{id}/reset-stats")
    public String resetUserStats(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.resetUserStats(id),
                "Đã đặt lại điểm và thống kê người chơi.", "redirect:/admin/users/" + id, redirectAttributes);
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails customUserDetails,
                             RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.deleteUser(id, customUserDetails.getUsername()),
                "Đã xóa tài khoản.", "redirect:/admin/users", redirectAttributes);
    }

    @PostMapping("/admin/rooms/{id}/close")
    public String closeRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.closeRoom(id),
                "Đã force đóng phòng và dọn trạng thái.", "redirect:/admin/rooms/" + id, redirectAttributes);
    }

    @PostMapping("/admin/rooms/{id}/reset")
    public String resetRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.resetRoom(id),
                "Đã reset phòng về trạng thái trống.", "redirect:/admin/rooms/" + id, redirectAttributes);
    }

    @PostMapping("/admin/rooms/{id}/delete")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.deleteRoom(id),
                "Đã xóa phòng.", "redirect:/admin/rooms", redirectAttributes);
    }

    @PostMapping("/admin/games/{id}/cancel")
    public String cancelGame(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.cancelGame(id),
                "Đã hủy trận đấu lỗi.", "redirect:/admin/games/" + id, redirectAttributes);
    }

    @PostMapping("/admin/games/{id}/delete")
    public String deleteGame(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.deleteGame(id),
                "Đã xóa lịch sử trận chưa hoàn tất.", "redirect:/admin/games", redirectAttributes);
    }

    @PostMapping("/admin/messages/{id}/delete")
    public String deleteMessage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return runAdminAction(() -> adminService.deleteMessage(id),
                "Đã xóa tin nhắn.", "redirect:/admin/messages", redirectAttributes);
    }

    private void populateAdminShell(Model model,
                                    CustomUserDetails customUserDetails,
                                    String activePage,
                                    String pageTitle) {
        model.addAttribute("adminUser", userService.getPersistedUser(customUserDetails.getUser()));
        model.addAttribute("activePage", activePage);
        model.addAttribute("pageTitle", pageTitle);
    }

    private String runAdminAction(AdminAction action,
                                  String successMessage,
                                  String redirect,
                                  RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("adminSuccess", successMessage);
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("adminError", exception.getMessage());
        }
        return redirect;
    }

    @FunctionalInterface
    private interface AdminAction {
        void run();
    }
}
