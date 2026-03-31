package com.example.caro_plus.service;

import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    @Autowired
    private UserRepository userRepository;

    // tính ELO
    public int calculateElo(int playerRating, int opponentRating, double score) {
        int K = 32;

        double expected = 1.0 / (1 + Math.pow(10, (opponentRating - playerRating) / 400.0));

        return (int) (playerRating + K * (score - expected));
    }

    // update rank
    public void updateRank(User playerX, User playerO, User winner) {

        double scoreX, scoreO;

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