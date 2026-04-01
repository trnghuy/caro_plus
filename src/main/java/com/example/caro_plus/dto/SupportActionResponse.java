package com.example.caro_plus.dto;

import lombok.Data;

@Data
public class SupportActionResponse {
    private String message;
    private double supportPoints;
    private Integer suggestedX;
    private Integer suggestedY;
}
