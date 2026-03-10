package com.example.caro_plus.model;

import java.util.Date;
import jakarta.persistence.*;
// import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomCode;

    @ManyToOne
    @JoinColumn(name = "host_id")
    private User host;

    private String status;
    
    private Date createdAt;
}
