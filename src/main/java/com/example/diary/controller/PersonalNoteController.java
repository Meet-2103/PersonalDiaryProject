package com.example.diary.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.diary.dto.UserNoteBatchRequest;
import com.example.diary.entity.DailyUserNote;
import com.example.diary.service.DiarySearchService;
import com.example.diary.service.NoteIngestionService;
import com.example.diary.service.NoteSummarizationService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/notes")
public class PersonalNoteController {

    private final NoteIngestionService noteIngestionService;
    private final DiarySearchService diarySearchService;
    private final NoteSummarizationService noteSummarizationService;

    public PersonalNoteController(NoteIngestionService noteIngestionService, DiarySearchService diarySearchService, NoteSummarizationService noteSummarizationService) {
        this.noteIngestionService = noteIngestionService;
        this.diarySearchService = diarySearchService;
        this.noteSummarizationService = noteSummarizationService;
    }

    @PostMapping
    public ResponseEntity<List<DailyUserNote>> ingestUserNotes(@RequestBody UserNoteBatchRequest request) {
        List<DailyUserNote> ingestedNotes = noteIngestionService.processAndIngestBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ingestedNotes);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchNotes(@RequestParam String query) {
        return ResponseEntity.ok(diarySearchService.searchDiary(query));
    }

    @GetMapping("/on-this-day")
    public ResponseEntity<List<DailyUserNote>> getOnThisDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(diarySearchService.getOnThisDayNotes(date));
    }

    @GetMapping("/prompt-me")
    public ResponseEntity<String> promptMe() {
        return ResponseEntity.ok(noteSummarizationService.generateProactivePrompt());
    }

    @PostMapping("/audio")
    public ResponseEntity<String> ingestAudio(@RequestParam("file") MultipartFile file) {
        // Here you would use Gemini Multimodal or OpenAI Whisper to transcribe the file.
        // For Gemini, you pass the bytes as Media to ChatClient, ask for a transcription, 
        // then route that text to noteIngestionService.processAndIngestBatch().
        return ResponseEntity.status(HttpStatus.CREATED).body("Audio ingestion placeholder for Gemini Multimodal");
    }
}
