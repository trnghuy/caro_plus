package com.example.caro_plus.model;

import java.util.Date;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "password is required")
    private String password;

    private Date createdAt;

    private int rating = 1000; // điểm ELO
    private int win = 0;
    private int lose = 0;
    private int draw = 0;
}
