package com.example.caro_plus.repository;

import com.example.caro_plus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
