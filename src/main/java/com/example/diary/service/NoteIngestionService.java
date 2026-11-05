package com.example.diary.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.diary.dto.ExtractedData;
import com.example.diary.dto.NoteSectionDto;
import com.example.diary.dto.UserNoteBatchRequest;
import com.example.diary.entity.DailyUserNote;
import com.example.diary.entity.NoteEntityExtraction;
import com.example.diary.repository.DailyUserNoteRepository;
import com.example.diary.repository.NoteEntityExtractionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NoteIngestionService {

    private final DailyUserNoteRepository noteRepository;
    private final NoteEntityExtractionRepository extractionRepository;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public NoteIngestionService(DailyUserNoteRepository noteRepository,
            NoteEntityExtractionRepository extractionRepository,
            VectorStore vectorStore,
            TokenTextSplitter textSplitter,
            ChatClient.Builder chatClientBuilder,
            ObjectMapper objectMapper) {
        this.noteRepository = noteRepository;
        this.extractionRepository = extractionRepository;
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<DailyUserNote> processAndIngestBatch(UserNoteBatchRequest request) {
        List<DailyUserNote> savedNotes = new ArrayList<>();
        List<Document> documentsToEmbed = new ArrayList<>();

        for (NoteSectionDto section : request.sections()) {
            DailyUserNote note = new DailyUserNote();
            note.setNoteDate(request.entryDate());
            note.setClientTimestamp(request.clientTimestamp());
            note.setTimezone(request.timezone());
            note.setCategory(section.category());
            note.setContent(section.content());

            DailyUserNote savedNote = noteRepository.save(note);
            savedNotes.add(savedNote);

            Map<String, Object> metadata = Map.of(
                    "note_id", savedNote.getId(),
                    "entry_date", request.entryDate().toString(),
                    "client_timestamp", request.clientTimestamp(),
                    "timezone", request.timezone() != null ? request.timezone() : "UTC",
                    "category", section.category(),
                    "context_type", "personal_note");

            Document rawDocument = new Document(section.content(), metadata);
            List<Document> chunks = textSplitter.apply(List.of(rawDocument));
            documentsToEmbed.addAll(chunks);

            // Trigger Async Extraction (Features 3 & 7)
            // extractEntitiesAndTripletsAsync(savedNote);
        }

        if (!documentsToEmbed.isEmpty()) {
            vectorStore.add(documentsToEmbed);
        }

        return savedNotes;
    }

    @Async
    public void extractEntitiesAndTripletsAsync(DailyUserNote note) {
        try {
            BeanOutputConverter<ExtractedData> converter = new BeanOutputConverter<>(ExtractedData.class);
            String prompt = """
                    Analyze the following diary entry. Extract any people mentioned, locations visited, and rate the overall mood from 1 to 10.
                    Also, extract relational triplets (Subject, Predicate, Object) to form a basic knowledge graph.

                    Entry: {entry}

                    {format}
                    """;

            PromptTemplate template = new PromptTemplate(prompt);
            String response = chatClient.prompt(template.create(Map.of(
                    "entry", note.getContent(),
                    "format", converter.getFormat()))).call().content();
            ExtractedData data = converter.convert(response);

            if (data != null) {
                NoteEntityExtraction extraction = new NoteEntityExtraction();
                extraction.setDailyUserNote(note);
                extraction.setPeople(String.join(",", data.people()));
                extraction.setLocations(String.join(",", data.locations()));
                extraction.setMoodScore(data.moodScore());
                extraction.setTripletsJson(objectMapper.writeValueAsString(data.triplets()));

                extractionRepository.save(extraction);
            }
        } catch (Exception e) {
            // Log failure, but don't disrupt main flow
            System.err.println("Failed to extract entities for note " + note.getId() + ": " + e.getMessage());
        }
    }
}