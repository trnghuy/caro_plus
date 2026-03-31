package com.example.caro_plus.controller;

import com.example.caro_plus.model.GameMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    // Khi Client gửi tới /app/game.join/{roomId}
    @MessageMapping("/game.join/{roomId}")
    // Server sẽ phát lại tới /topic/room/{roomId}
    @SendTo("/topic/room/{roomId}")
    public GameMessage joinRoom(@DestinationVariable String roomId, GameMessage message) {
        // Gán loại tin nhắn là JOIN để Client biết đường xử lý
        message.setType("JOIN");
        System.out.println("User " + message.getSender() + " đã tham gia phòng: " + roomId);
        return message; 
    }

    // Khi Client gửi tới /app/game.move/{roomId}
    @MessageMapping("/game.move/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public GameMessage makeMove(@DestinationVariable String roomId, GameMessage message) {
        message.setType("MOVE");
        // Tạm thời chỉ chuyển tiếp tọa độ x, y để 2 máy thấy nhau
        return message;
    }
}