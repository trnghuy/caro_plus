package com.example.caro_plus.service;

import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.GameRepository;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Game createGame(Room room) {
        Game game = new Game();
        game.setRoom(room);
        game.setPlayerX(room.getHost());
        game.setPlayerO(room.getPlayer2());
        game.setStatus("PLAYING");
        game.setWinner(null);
        return gameRepository.save(game);
    }

    @Transactional
    public void finishGame(Room room, String winnerUsername) {
        gameRepository.findTopByRoomAndStatusOrderByIdDesc(room, "PLAYING")
                .ifPresent(game -> {
                    User playerX = game.getPlayerX();
                    User playerO = game.getPlayerO();

                    User winner = null;
                    if (winnerUsername != null && !winnerUsername.isBlank()) {
                        winner = userRepository.findByUsername(winnerUsername)
                                .orElseThrow(() -> new RuntimeException("Winner khong ton tai"));
                    }

                    game.setWinner(winner);
                    game.setStatus("FINISHED");
                    gameRepository.save(game);

                    if (playerX != null && playerO != null) {
                        updateRank(playerX, playerO, winner);
                    }
                });
    }

    public int calculateElo(int playerRating, int opponentRating, double score) {
        int k = 32;
        double expected = 1.0 / (1 + Math.pow(10, (opponentRating - playerRating) / 400.0));
        return (int) (playerRating + k * (score - expected));
    }

    public void updateRank(User playerX, User playerO, User winner) {
        double scoreX;
        double scoreO;

        if (winner == null) {
            scoreX = 0.5;
            scoreO = 0.5;
            playerX.setDraw(playerX.getDraw() + 1);
            playerO.setDraw(playerO.getDraw() + 1);
        } else if (winner.getId().equals(playerX.getId())) {
            scoreX = 1;
            scoreO = 0;
            playerX.setWin(playerX.getWin() + 1);
            playerO.setLose(playerO.getLose() + 1);
        } else {
            scoreX = 0;
            scoreO = 1;
            playerO.setWin(playerO.getWin() + 1);
            playerX.setLose(playerX.getLose() + 1);
        }

        int newX = calculateElo(playerX.getRating(), playerO.getRating(), scoreX);
        int newO = calculateElo(playerO.getRating(), playerX.getRating(), scoreO);

        playerX.setRating(newX);
        playerO.setRating(newO);

        userRepository.save(playerX);
        userRepository.save(playerO);
    }
}
