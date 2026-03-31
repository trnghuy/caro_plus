package com.example.caro_plus.config;

import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RankingMigrationRunner implements ApplicationRunner {

    private final UserRepository userRepository;

    public RankingMigrationRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<User> users = userRepository.findAll();
        boolean changed = false;

        for (User user : users) {
            double normalizedPoints = Math.max(0, Math.round(user.getSupportPoints() * 2.0) / 2.0);
            if (normalizedPoints >= 100) {
                normalizedPoints = 5.0;
            }

            if (Double.compare(user.getSupportPoints(), normalizedPoints) != 0) {
                user.setSupportPoints(normalizedPoints);
                changed = true;
            }

            int derivedRankScore = user.getWin() * 3 + user.getDraw();
            if (user.getRankScore() == 0 && (user.getWin() > 0 || user.getLose() > 0 || user.getDraw() > 0)) {
                user.setRankScore(derivedRankScore);
                changed = true;
            } else if (user.getRankScore() < 0) {
                user.setRankScore(0);
                changed = true;
            }
        }

        if (changed) {
            userRepository.saveAll(users);
        }
    }
}
