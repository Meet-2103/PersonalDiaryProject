package com.example.diary.dto;

import java.util.List;

public record ExtractedData(
        List<String> people,
        List<String> locations,
        Integer moodScore,
        List<Triplet> triplets
) {
    public record Triplet(String subject, String predicate, String object) {}
}
