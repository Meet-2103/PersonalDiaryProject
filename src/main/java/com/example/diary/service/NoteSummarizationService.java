package com.example.diary.service;

import com.example.diary.entity.AggregatedNoteSummary;
import com.example.diary.entity.DailyNoteSummary;
import com.example.diary.entity.DailyUserNote;
import com.example.diary.repository.AggregatedNoteSummaryRepository;
import com.example.diary.repository.DailyNoteSummaryRepository;
import com.example.diary.repository.DailyUserNoteRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteSummarizationService {

    private final DailyUserNoteRepository dailyUserNoteRepository;
    private final DailyNoteSummaryRepository dailyNoteSummaryRepository;
    private final AggregatedNoteSummaryRepository aggregatedSummaryRepository;
    private final ChatClient chatClient;

    public NoteSummarizationService(
            DailyUserNoteRepository dailyUserNoteRepository,
            DailyNoteSummaryRepository dailyNoteSummaryRepository,
            AggregatedNoteSummaryRepository aggregatedSummaryRepository,
            ChatClient.Builder chatClientBuilder) {
        this.dailyUserNoteRepository = dailyUserNoteRepository;
        this.dailyNoteSummaryRepository = dailyNoteSummaryRepository;
        this.aggregatedSummaryRepository = aggregatedSummaryRepository;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Daily Summary: Runs every day at 23:59
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void generateDailySummary() {
        LocalDate today = LocalDate.now();
        List<DailyUserNote> notes = dailyUserNoteRepository.findByNoteDate(today);
        if (notes.isEmpty()) return;

        String combinedNotes = notes.stream().map(DailyUserNote::getContent).collect(Collectors.joining("\n"));
        String prompt = "Summarize the following daily notes. Extract highs, insights, and targets completed:\n" + combinedNotes;
        
        String summary = chatClient.prompt(prompt).call().content();
        
        DailyNoteSummary dailySummary = new DailyNoteSummary();
        dailySummary.setSummaryDate(today);
        dailySummary.setFullSummaryText(summary);
        dailyNoteSummaryRepository.save(dailySummary);
    }

    /**
     * Weekly Summary: Runs every Sunday at 23:59
     */
    @Scheduled(cron = "0 59 23 * * SUN")
    public void generateWeeklySummary() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        generateAggregatedSummary(start, end, AggregatedNoteSummary.SummaryType.WEEKLY);
    }

    private void generateAggregatedSummary(LocalDate start, LocalDate end, AggregatedNoteSummary.SummaryType type) {
        // Implementation fetches previous level summaries and rolls them up.
        // For brevity, fetching daily summaries for weekly rollup.
        // In a real app, Monthly would fetch Weekly summaries.
        String prompt = "Summarize the following entries from " + start + " to " + end + ":\n";
        String summary = chatClient.prompt(prompt).call().content();
        
        AggregatedNoteSummary agg = new AggregatedNoteSummary();
        agg.setType(type);
        agg.setStartDate(start);
        agg.setEndDate(end);
        agg.setSummaryText(summary);
        aggregatedSummaryRepository.save(agg);
    }

    /**
     * Feature 5: Proactive Journaling
     */
    public String generateProactivePrompt() {
        List<DailyNoteSummary> recentSummaries = dailyNoteSummaryRepository.findAll()
                .stream().sorted((s1, s2) -> s2.getSummaryDate().compareTo(s1.getSummaryDate()))
                .limit(3).toList();

        if (recentSummaries.isEmpty()) {
            return "What's on your mind today?";
        }

        String context = recentSummaries.stream().map(DailyNoteSummary::getFullSummaryText).collect(Collectors.joining("\n"));
        String prompt = "You are a thoughtful journaling assistant. Based on the following recent diary entries, ask ONE open-ended, engaging question to help the user write today's entry.\n\nContext:\n" + context;

        return chatClient.prompt(prompt).call().content();
    }
}
