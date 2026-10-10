package com.example.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import com.example.diary.entity.DailyUserNote;

public interface DailyUserNoteRepository extends JpaRepository<DailyUserNote, Long> {
    List<DailyUserNote> findByNoteDate(LocalDate noteDate);
    List<DailyUserNote> findByNoteDateAndCategory(LocalDate noteDate, String category);
    List<DailyUserNote> findByNoteDateIn(List<LocalDate> dates);
    List<DailyUserNote> findByNoteDateBetween(LocalDate start, LocalDate end);
}