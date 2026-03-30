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
@Table(name = "room")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "host_id")
    private User host;

    @ManyToOne
    @JoinColumn(name = "player2_id")
    private User player2;

    private String status;
    
    private Date createdAt;
}
