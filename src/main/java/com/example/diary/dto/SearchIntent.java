package com.example.diary.dto;

import java.time.LocalDate;

public record SearchIntent(
        IntentType type,
        LocalDate startDate,
        LocalDate endDate,
        String category,
        String searchTerms
) {
    public enum IntentType {
        SQL_ONLY, // Use when user asks for exact dates/times
        HYBRID    // Use when user asks semantic questions with or without timeframes
    }
}
