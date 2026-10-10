# Implementation Plan: AI Diary Features

## Goal Description
We are upgrading the AI Diary application to include 8 advanced features: Hybrid Search, Time-Based Retrieval, Map-Reduce Summarization, Entity Extraction, "On This Day" flashbacks, Proactive Journaling prompts, Voice Ingestion (using Gemini Multimodal), and Triplet Extraction (GraphRAG-lite). This plan outlines the concrete code changes required to build these features using Spring Boot, Spring AI, and the Gemini API.

## User Review Required
> [!IMPORTANT]
> **API Key Setup:** To execute this plan, we will need the `spring.ai.google.genai.api-key` set in your `application.properties` (or as an environment variable). Please ensure you have a free Gemini API key ready.
> **Audio Ingestion (Feature 6):** Since we are using Gemini, we will use Gemini's native multimodal capabilities to transcribe audio rather than OpenAI's Whisper.

## Open Questions
> [!NOTE]
> Are you comfortable using standard PostgreSQL to store extracted GraphRAG triplets (Subject, Predicate, Object) for now, instead of spinning up a dedicated Neo4j Graph database? (PostgreSQL is easier and free to start with).

---

## Proposed Changes

### 1. Dependencies and Configuration
Update Spring Boot configuration to enable async operations and scheduling, and ensure Gemini API is fully wired up.

#### [MODIFY] `src/main/java/com/example/diary/DiaryApplication.java`
- Add `@EnableScheduling` and `@EnableAsync` annotations to allow background jobs (summaries) and async processing (entity extraction).

#### [MODIFY] `src/main/resources/application.properties`
- Add placeholder `spring.ai.google.genai.api-key=${GEMINI_API_KEY}`.

---

### 2. Time-Based Retrieval & "On This Day" (Features 4 & 8)
Add repository methods and a new service to fetch historical notes without hitting the vector database.

#### [MODIFY] `src/main/java/com/example/diary/repository/DailyUserNoteRepository.java`
- Add method: `List<DailyUserNote> findByNoteDateIn(List<LocalDate> dates);`
- Add method: `List<DailyUserNote> findByNoteDateBetween(LocalDate start, LocalDate end);`

#### [NEW] `src/main/java/com/example/diary/service/DiarySearchService.java`
- Implement `getOnThisDayNotes(LocalDate today)` to fetch notes from exactly 1, 2, and 5 years ago.
- Expose via `GET /api/v1/notes/on-this-day`.

---

### 3. Hybrid Search / Query Router (Feature 1)
Use a Gemini prompt to analyze user queries and decide whether to use SQL (time-based) or Vector Search (semantic).

#### [MODIFY] `src/main/java/com/example/diary/service/DiarySearchService.java`
- Add a `ChatClient` prompt to classify the intent: "Is this query asking for a specific date range, or a semantic concept?"
- If date range -> Use SQL. If semantic -> Use Vector DB with metadata filters.

---

### 4. Long-Term Summarization (Feature 2)
Implement the Map-Reduce cron jobs.

#### [NEW] `src/main/java/com/example/diary/entity/AggregatedNoteSummary.java`
- Create an entity to store WEEKLY, MONTHLY, and YEARLY summaries (since `DailyNoteSummary` already exists).

#### [NEW] `src/main/java/com/example/diary/service/NoteSummarizationService.java`
- Implement `@Scheduled(cron = "0 59 23 * * ?")` for daily summaries.
- Implement `@Scheduled(cron = "0 0 0 * * SUN")` for weekly summaries.
- Implement `@Scheduled(cron = "0 0 0 1 * ?")` for monthly summaries.

---

### 5. Entity & Graph Triplet Extraction (Features 3 & 7)
After a note is saved, asynchronously extract People, Places, Moods, and relational Triplets.

#### [NEW] `src/main/java/com/example/diary/entity/NoteEntityExtraction.java`
- Entity to store `person`, `location`, `mood_score`, `triplets_json` linked to the `DailyUserNote`.

#### [MODIFY] `src/main/java/com/example/diary/service/NoteIngestionService.java`
- Add a `@Async` method `extractEntitiesAndTriplets(DailyUserNote note)` that fires after saving the note to PostgreSQL.
- Uses `BeanOutputConverter` to enforce Gemini to return a structured JSON response.

---

### 6. Proactive Journaling (Feature 5)
Generate contextual prompts for the user before they write.

#### [MODIFY] `src/main/java/com/example/diary/controller/PersonalNoteController.java`
- Add endpoint `GET /api/v1/notes/prompt-me`.

#### [MODIFY] `src/main/java/com/example/diary/service/NoteSummarizationService.java`
- Add `generateProactivePrompt()` which reads the last 3 `DailyNoteSummary` records and asks Gemini for an engaging follow-up question.

---

### 7. Voice Ingestion (Feature 6)
Allow audio uploads.

#### [MODIFY] `src/main/java/com/example/diary/controller/PersonalNoteController.java`
- Add `POST /api/v1/notes/audio` accepting a `MultipartFile`.
- Pass the audio file as a Spring AI `Media` object to Gemini Multimodal, asking it to transcribe and format the text, then pass the transcribed text to the `NoteIngestionService`.

---

## Verification Plan

### Manual Verification
1.  Upload a batch of notes via POST `/api/v1/notes`.
2.  Check the database to verify that `NoteEntityExtraction` asynchronously populated with the correct entities.
3.  Hit GET `/api/v1/notes/on-this-day` and confirm it fetches historical dates.
4.  Hit GET `/api/v1/notes/prompt-me` and ensure it returns a sensible question.
5.  Send a test audio file to `/api/v1/notes/audio` and verify it is transcribed and ingested.
