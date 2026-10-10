package com.example.diary.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "note_entity_extractions")
public class NoteEntityExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "note_id", referencedColumnName = "id")
    private DailyUserNote dailyUserNote;

    @Column(name = "people", columnDefinition = "TEXT")
    private String people;

    @Column(name = "locations", columnDefinition = "TEXT")
    private String locations;

    @Column(name = "mood_score")
    private Integer moodScore;

    @Column(name = "triplets_json", columnDefinition = "TEXT")
    private String tripletsJson; // To simulate GraphRAG without Neo4j

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DailyUserNote getDailyUserNote() { return dailyUserNote; }
    public void setDailyUserNote(DailyUserNote dailyUserNote) { this.dailyUserNote = dailyUserNote; }
    public String getPeople() { return people; }
    public void setPeople(String people) { this.people = people; }
    public String getLocations() { return locations; }
    public void setLocations(String locations) { this.locations = locations; }
    public Integer getMoodScore() { return moodScore; }
    public void setMoodScore(Integer moodScore) { this.moodScore = moodScore; }
    public String getTripletsJson() { return tripletsJson; }
    public void setTripletsJson(String tripletsJson) { this.tripletsJson = tripletsJson; }
}
