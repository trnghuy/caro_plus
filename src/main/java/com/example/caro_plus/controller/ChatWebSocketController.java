package com.example.caro_plus.controller;

import com.example.caro_plus.dto.MessageResponse;
import com.example.caro_plus.dto.SendMessageRequest;
import com.example.caro_plus.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatMessageService chatMessageService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/rooms/{roomId}/chat.send")
    public void sendChatMessage(@DestinationVariable Long roomId,
                                SendMessageRequest request,
                                Principal principal) {
        String authenticatedUsername = principal == null ? null : principal.getName();
        MessageResponse savedMessage = chatMessageService.sendMessage(roomId, request, authenticatedUsername);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/chat", savedMessage);
    }
}
