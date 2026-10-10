package com.example.diary.repository;

import com.example.diary.entity.NoteEntityExtraction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteEntityExtractionRepository extends JpaRepository<NoteEntityExtraction, Long> {
}
