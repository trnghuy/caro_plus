package com.example.caro_plus.service;

import com.example.caro_plus.model.User;
import com.example.caro_plus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // LOGIN
    public User login(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // so sánh password (hiện tại plain text)
            if (user.getPassword().equals(password)) {
                return user;
            }
        }

        return null;
    }

    // REGISTER
    public void register(User user) {
        user.setCreatedAt(new Date());
        userRepository.save(user);
    }

    // CHECK USERNAME
    public User findByUsername(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        return optionalUser.orElse(null);
    }
}