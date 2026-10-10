package com.example.diary.repository;

import com.example.diary.entity.AggregatedNoteSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AggregatedNoteSummaryRepository extends JpaRepository<AggregatedNoteSummary, Long> {
    List<AggregatedNoteSummary> findByTypeAndStartDateBetween(AggregatedNoteSummary.SummaryType type, LocalDate start, LocalDate end);
}
