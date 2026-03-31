package com.example.caro_plus.service;

import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User login(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Dùng passwordEncoder để check mật khẩu đã mã hóa
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(new Date());
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        return optionalUser.orElse(null);
    }

    public User getPersistedUser(User user) {
        if (user == null) {
            return null;
        }

        if (user.getId() != null) {
            Optional<User> byId = userRepository.findById(user.getId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return findByUsername(user.getUsername());
        }

        return null;
    }
}
