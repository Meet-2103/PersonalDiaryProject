package com.example.diary.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_note_summaries")
public class DailyNoteSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summary_date", nullable = false, unique = true)
    private LocalDate summaryDate;

    @Column(name = "highs", columnDefinition = "TEXT")
    private String highs;

    @Column(name = "target_completions", columnDefinition = "TEXT")
    private String targetCompletions;

    @Column(name = "insights", columnDefinition = "TEXT")
    private String insights;

    @Column(name = "full_summary_text", nullable = false, columnDefinition = "TEXT")
    private String fullSummaryText;

    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onGenerate() {
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    public String getHighs() { return highs; }
    public void setHighs(String highs) { this.highs = highs; }
    public String getTargetCompletions() { return targetCompletions; }
    public void setTargetCompletions(String targetCompletions) { this.targetCompletions = targetCompletions; }
    public String getInsights() { return insights; }
    public void setInsights(String insights) { this.insights = insights; }
    public String getFullSummaryText() { return fullSummaryText; }
    public void setFullSummaryText(String fullSummaryText) { this.fullSummaryText = fullSummaryText; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}