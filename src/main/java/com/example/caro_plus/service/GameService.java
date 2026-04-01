package com.example.caro_plus.service;

import com.example.caro_plus.model.Game;
import com.example.caro_plus.model.Move;
import com.example.caro_plus.model.Room;
import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.GameRepository;
import com.example.caro_plus.repository.MoveRepository;
import com.example.caro_plus.repository.RoomRepository;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MoveRepository moveRepository;

    @Transactional
    public Game createGame(Room room) {
        room.setStatus("playing");
        Room savedRoom = roomRepository.save(room);

        Game game = new Game();
        game.setRoom(savedRoom);
        game.setPlayerX(savedRoom.getHost());
        game.setPlayerO(savedRoom.getPlayer2());
        game.setStatus("PLAYING");
        game.setWinner(null);
        game.setCreatedAt(new Date());
        game.setFinishedAt(null);
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
                    game.setFinishedAt(new Date());
                    gameRepository.save(game);

                    // After a match ends, the room should no longer stay stuck in "playing".
                    // Keep both players in the room so they can replay or leave cleanly.
                    room.setStatus("full");
                    roomRepository.save(room);

                    if (playerX != null && playerO != null) {
                        updateRank(playerX, playerO, winner);
                    }
                });
    }

    private double adjustSupportPoints(double currentPoints, double delta) {
        double updated = currentPoints + delta;
        if (updated < 0) {
            updated = 0;
        }
        return Math.round(updated * 2.0) / 2.0;
    }

    private int adjustRankScore(int currentScore, int delta) {
        int updated = currentScore + delta;
        return Math.max(updated, 0);
    }

    @Transactional
    public double spendSupportPoints(String username, double cost) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nguoi choi khong ton tai"));

        if (user.getSupportPoints() < cost) {
            throw new IllegalStateException("Bạn không đủ sao để sử dụng trợ giúp này.");
        }

        user.setSupportPoints(adjustSupportPoints(user.getSupportPoints(), -cost));
        userRepository.save(user);
        return user.getSupportPoints();
    }

    @Transactional
    public void recordMove(Room room, String username, int x, int y) {
        if (room == null || username == null || username.isBlank()) {
            return;
        }

        Game game = gameRepository.findTopByRoomAndStatusOrderByIdDesc(room, "PLAYING").orElse(null);
        User player = userRepository.findByUsername(username).orElse(null);
        if (game == null || player == null) {
            return;
        }

        Move move = new Move();
        move.setGame(game);
        move.setPlayer(player);
        move.setX(x);
        move.setY(y);
        move.setMoveOrder((int) moveRepository.countByGame(game) + 1);
        moveRepository.save(move);
    }

    @Transactional
    public void removeLastMoves(Room room, int count) {
        if (room == null || count <= 0) {
            return;
        }

        Game game = gameRepository.findTopByRoomAndStatusOrderByIdDesc(room, "PLAYING").orElse(null);
        if (game == null) {
            return;
        }

        List<Move> moves = moveRepository.findTop2ByGameOrderByMoveOrderDesc(game);
        if (moves.isEmpty()) {
            return;
        }

        moveRepository.deleteAll(moves.stream().limit(count).toList());
    }

    public void updateRank(User playerX, User playerO, User winner) {
        if (winner == null) {
            playerX.setSupportPoints(adjustSupportPoints(playerX.getSupportPoints(), 0.5));
            playerO.setSupportPoints(adjustSupportPoints(playerO.getSupportPoints(), 0.5));
            playerX.setRankScore(adjustRankScore(playerX.getRankScore(), 1));
            playerO.setRankScore(adjustRankScore(playerO.getRankScore(), 1));
            playerX.setDraw(playerX.getDraw() + 1);
            playerO.setDraw(playerO.getDraw() + 1);
        } else if (winner.getId().equals(playerX.getId())) {
            playerX.setSupportPoints(adjustSupportPoints(playerX.getSupportPoints(), 1));
            playerO.setSupportPoints(adjustSupportPoints(playerO.getSupportPoints(), -1));
            playerX.setRankScore(adjustRankScore(playerX.getRankScore(), 3));
            playerO.setRankScore(adjustRankScore(playerO.getRankScore(), -1));
            playerX.setWin(playerX.getWin() + 1);
            playerO.setLose(playerO.getLose() + 1);
        } else {
            playerO.setSupportPoints(adjustSupportPoints(playerO.getSupportPoints(), 1));
            playerX.setSupportPoints(adjustSupportPoints(playerX.getSupportPoints(), -1));
            playerO.setRankScore(adjustRankScore(playerO.getRankScore(), 3));
            playerX.setRankScore(adjustRankScore(playerX.getRankScore(), -1));
            playerO.setWin(playerO.getWin() + 1);
            playerX.setLose(playerX.getLose() + 1);
        }

        userRepository.save(playerX);
        userRepository.save(playerO);
    }
}
