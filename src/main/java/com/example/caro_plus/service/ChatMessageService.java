package com.example.caro_plus.service;

import com.example.caro_plus.dto.MessageResponse;
import com.example.caro_plus.dto.SendMessageRequest;
import com.example.caro_plus.model.ChatMessage;
import com.example.caro_plus.model.MessageType;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.ChatMessageRepository;
import com.example.caro_plus.repository.RoomPlayerRepository;
import com.example.caro_plus.repository.RoomRepository;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

@Service
public class ChatMessageService {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final ChatMessageRepository chatMessageRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomPlayerRepository roomPlayerRepository;

    public ChatMessageService(ChatMessageRepository chatMessageRepository,
                              RoomRepository roomRepository,
                              UserRepository userRepository,
                              RoomPlayerRepository roomPlayerRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomPlayerRepository = roomPlayerRepository;
    }

    public List<MessageResponse> getMessagesByRoom(Long roomId) {
        Room room = getRoomOrThrow(roomId);

        return chatMessageRepository.findByRoomOrderByCreatedAtAscIdAsc(room)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MessageResponse sendMessage(Long roomId, SendMessageRequest request, String authenticatedUsername) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message request is required");
        }

        Room room = getRoomOrThrow(roomId);
        User sender = resolveSender(authenticatedUsername, request.getSenderId());

        if (!isRoomMember(room, sender)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender is not in this room");
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content must not be blank");
        }

        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content must be at most 500 characters");
        }

        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(content);
        message.setType(MessageType.TEXT);
        message.setCreatedAt(new Date());

        return toResponse(chatMessageRepository.save(message));
    }

    public MessageResponse createSystemMessage(Long roomId, String content) {
        Room room = getRoomOrThrow(roomId);

        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System message content must not be blank");
        }

        User systemSender = room.getHost();
        if (systemSender == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System sender not available");
        }

        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(systemSender);
        message.setContent(normalizedContent);
        message.setType(MessageType.SYSTEM);
        message.setCreatedAt(new Date());

        return toResponse(chatMessageRepository.save(message));
    }

    private Room getRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private User resolveSender(String authenticatedUsername, Long senderIdFallback) {
        if (authenticatedUsername != null && !authenticatedUsername.isBlank()) {
            return userRepository.findByUsername(authenticatedUsername)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found"));
        }

        if (senderIdFallback != null) {
            return userRepository.findById(senderIdFallback)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found"));
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender is required");
    }

    private boolean isRoomMember(Room room, User user) {
        if (room.getHost() != null && room.getHost().getId() != null && room.getHost().getId().equals(user.getId())) {
            return true;
        }

        return roomPlayerRepository.existsByRoomAndPlayer(room, user);
    }

    private MessageResponse toResponse(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .content(message.getContent())
                .type(message.getType().name())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
