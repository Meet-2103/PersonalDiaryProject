# Diary API Controllers Documentation

This document provides a high-level overview of all the REST controllers in the Diary application, their responsibilities, and the step-by-step processes of their endpoints.

## 1. PersonalNoteController
**Location:** `com.example.diary.controller.PersonalNoteController`  
**Base Route:** `/api/v1/notes`

This is currently the sole controller in the application. It acts as the primary API interface for clients (like a frontend application or mobile app) to interact with the user's diary notes. It handles creating new notes, searching past notes, retrieving memories, and generating AI prompts.

### Endpoints and High-Level Steps

#### A. Ingest User Notes
*   **Method / Path:** `POST /`
*   **Purpose:** To accept a batch of new diary entries from the user and save them into the system.
*   **High-Level Steps:**
    1.  Receives a JSON payload mapped to a `UserNoteBatchRequest` object.
    2.  Passes the payload to `NoteIngestionService.processAndIngestBatch()`.
    3.  The service (behind the scenes) processes the notes, likely generating vector embeddings using Spring AI and extracting metadata.
    4.  The processed notes are saved to the PostgreSQL database.
    5.  Returns an HTTP `201 Created` response containing the list of successfully saved `DailyUserNote` objects.

#### B. Search Notes
*   **Method / Path:** `GET /search`
*   **Purpose:** To find specific past diary entries based on a user's search query.
*   **High-Level Steps:**
    1.  Extracts the `query` string parameter from the request URL.
    2.  Passes the query to `DiarySearchService.searchDiary()`.
    3.  The service executes a semantic/vector search against the vector database (finding notes conceptually similar to the query).
    4.  Returns an HTTP `200 OK` response containing the search results.

#### C. On This Day
*   **Method / Path:** `GET /on-this-day`
*   **Purpose:** A nostalgic feature that retrieves diary entries written on the same calendar day in previous years.
*   **High-Level Steps:**
    1.  Checks if a specific `date` parameter is provided in the request.
    2.  If no date is provided, it defaults to the current day (`LocalDate.now()`).
    3.  Passes the date to `DiarySearchService.getOnThisDayNotes()`.
    4.  The service queries the database for notes matching the specific month and day across all past years.
    5.  Returns an HTTP `200 OK` response with the matching `DailyUserNote` objects.

#### D. Prompt Me
*   **Method / Path:** `GET /prompt-me`
*   **Purpose:** Generates a personalized writing prompt using AI to help the user overcome writer's block.
*   **High-Level Steps:**
    1.  Receives the request without any parameters.
    2.  Calls `NoteSummarizationService.generateProactivePrompt()`.
    3.  The service leverages an LLM (like Gemini) to analyze recent user notes and context, producing a tailored, proactive question.
    4.  Returns an HTTP `200 OK` response containing the generated string prompt.

#### E. Ingest Audio Note (Placeholder)
*   **Method / Path:** `POST /audio`
*   **Purpose:** To accept a voice recording, transcribe it, and save it as a diary entry.
*   **High-Level Steps:**
    1.  Receives a `MultipartFile` containing the audio recording.
    2.  *(Currently unimplemented)* It is structured to pass the audio bytes to an AI model (like Gemini Multimodal or Whisper) for transcription.
    3.  *(Currently unimplemented)* The resulting text would be sent to the `NoteIngestionService` for standard processing.
    4.  Currently returns an HTTP `201 Created` response with a placeholder string message.
