package com.example.caro_plus.config;

import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean hasAdmin = userRepository.countByRole("ADMIN") > 0;
        if (hasAdmin) {
            return;
        }

        User admin = userRepository.findByUsername("admin").orElseGet(User::new);
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setCreatedAt(admin.getCreatedAt() == null ? new Date() : admin.getCreatedAt());
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin.setLocked(false);
        if (admin.getSupportPoints() < 5) {
            admin.setSupportPoints(5.0);
        }
        userRepository.save(admin);
    }
}
