package com.example.diary.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import com.example.diary.dto.SearchIntent;
import com.example.diary.entity.DailyUserNote;
import com.example.diary.repository.DailyUserNoteRepository;

@Service
public class DiarySearchService {

    private final DailyUserNoteRepository dailyUserNoteRepository;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public DiarySearchService(DailyUserNoteRepository dailyUserNoteRepository, ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore) {
        this.dailyUserNoteRepository = dailyUserNoteRepository;
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    /**
     * Feature 4: Automated "On This Day"
     */
    public List<DailyUserNote> getOnThisDayNotes(LocalDate today) {
        List<LocalDate> historicalDates = List.of(
                today.minusYears(1),
                today.minusYears(2),
                today.minusYears(5));
        return dailyUserNoteRepository.findByNoteDateIn(historicalDates);
    }

    /**
     * Feature 8: Time-Based Retrieval (Bypass Vector Search)
     */
    public List<DailyUserNote> getTimeBasedNotes(LocalDate start, LocalDate end) {
        return dailyUserNoteRepository.findByNoteDateBetween(start, end);
    }

    /**
     * Feature 1 & 8: Query Router and Hybrid Search
     */
    public Object searchDiary(String query) {
        // 1. Ask Gemini to classify the query intent
        SearchIntent intent = routeQuery(query);

        // 2. Execute based on Intent
        if (intent.type() == SearchIntent.IntentType.SQL_ONLY) {
            // Bypass Vector DB (Feature 8)
            LocalDate start = intent.startDate() != null ? intent.startDate() : LocalDate.now().minusYears(100);
            LocalDate end = intent.endDate() != null ? intent.endDate() : LocalDate.now();
            System.out.println("In sql");
            return getTimeBasedNotes(start, end);
        } else {
            // Hybrid Search (Feature 1)
            SearchRequest request = SearchRequest.builder()
                    .query(intent.searchTerms() != null ? intent.searchTerms() : query).topK(5).build();

            FilterExpressionBuilder b = new FilterExpressionBuilder();
            // Optional: apply category or date filters if intent parsed them
            if (intent.category() != null && !intent.category().isBlank()) {
                request = SearchRequest.builder().query(intent.searchTerms() != null ? intent.searchTerms() : query)
                        .topK(5).filterExpression(b.eq("category", intent.category()).build()).build();
            }

            List<Document> semanticResults = vectorStore.similaritySearch(request);

            // Map chunks back to original DB entities if needed, or just return the text
            // chunks
            return semanticResults.stream().map(Document::getText).collect(Collectors.toList());
        }
    }

    private SearchIntent routeQuery(String query) {
        BeanOutputConverter<SearchIntent> converter = new BeanOutputConverter<>(SearchIntent.class);

        String prompt = """
                Analyze the following diary search query.
                Determine if the user is asking for an exact date/time range (SQL_ONLY) or a semantic/topic search (HYBRID).
                If HYBRID, extract any category (like work, personal) and specific search terms.
                If SQL_ONLY, try to extract startDate and endDate (use YYYY-MM-DD). If it's a single day, set both to that day.

                Query: {query}

                {format}
                """;

        PromptTemplate template = new PromptTemplate(prompt);
        String response = chatClient.prompt(template.create(Map.of(
                "query", query,
                "format", converter.getFormat()))).call().content();
        return converter.convert(response);
    }
}
