package com.example.diary.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

import com.example.diary.entity.DailyNoteSummary;

public interface DailyNoteSummaryRepository extends JpaRepository<DailyNoteSummary, Long> {
    Optional<DailyNoteSummary> findBySummaryDate(LocalDate summaryDate);
}