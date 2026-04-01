package com.example.caro_plus.controller;

import com.example.caro_plus.dto.MatchHistoryItemResponse;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.MatchHistoryService;
import com.example.caro_plus.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HistoryController {

    private final MatchHistoryService matchHistoryService;
    private final UserService userService;

    public HistoryController(MatchHistoryService matchHistoryService, UserService userService) {
        this.matchHistoryService = matchHistoryService;
        this.userService = userService;
    }

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal CustomUserDetails customUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        if (customUser == null) {
            return "redirect:/";
        }

        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = size <= 0 ? 10 : Math.min(size, 50);

        Page<MatchHistoryItemResponse> historyPage = matchHistoryService.getMatchHistoryForCurrentUser(
                customUser.getUsername(),
                PageRequest.of(sanitizedPage, sanitizedSize));

        model.addAttribute("user", userService.getPersistedUser(customUser.getUser()));
        model.addAttribute("historyPage", historyPage);
        model.addAttribute("historyItems", historyPage.getContent());
        model.addAttribute("pageSize", sanitizedSize);
        return "history";
    }
}
