package com.example.caro_plus.controller;

import com.example.caro_plus.dto.MessageResponse;
import com.example.caro_plus.dto.SendMessageRequest;
import com.example.caro_plus.security.CustomUserDetails;
import com.example.caro_plus.service.ChatMessageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
public class MessageApiController {

    private final ChatMessageService chatMessageService;

    public MessageApiController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping
    public List<MessageResponse> getMessages(@PathVariable Long roomId) {
        return chatMessageService.getMessagesByRoom(roomId);
    }

    @PostMapping
    public MessageResponse sendMessage(@PathVariable Long roomId,
                                       @RequestBody SendMessageRequest request,
                                       @AuthenticationPrincipal CustomUserDetails customUser) {
        String authenticatedUsername = customUser == null ? null : customUser.getUsername();
        return chatMessageService.sendMessage(roomId, request, authenticatedUsername);
    }
}
