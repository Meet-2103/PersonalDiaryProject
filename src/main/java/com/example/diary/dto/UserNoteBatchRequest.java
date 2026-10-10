package com.example.diary.dto;

import java.time.LocalDate;
import java.util.List;

public record UserNoteBatchRequest(
    LocalDate entryDate,
    String clientTimestamp,
    String timezone,
    List<NoteSectionDto> sections
) {}
