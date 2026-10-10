# AI Diary: Feature Implementation & Architecture Plan

This document outlines the detailed processes, workflows, and implementation strategies for the 8 features requested for the AI Diary application. We will review these designs before writing the code.

---

## 1. Hybrid Search (Metadata + Vectors) & Time-Based Retrieval (Bypass Vector Search)
*Note: Features 1 and 8 are deeply connected, as they both handle how we retrieve data based on user queries.*

### The Concept
When you ask a question like, "What did I do at work last week?", standard vector search might return notes about "work" from 3 years ago because they are semantically similar. To fix this, we use a **Query Router** and **Hybrid Search**.

### How it Works (The Process)
1. **Query Routing (LLM Decision):** When the user submits a query (e.g., "What happened on Oct 12, 2022?"), we first send the query to a lightweight LLM prompt (the "Router").
2. **Intent Classification:** The Router determines the intent:
    *   **Exact Date/Range (Feature 8):** If the query asks for a specific date or range (e.g., "Show me my entry for Jan 1st"), the Router outputs a JSON indicating a `SQL_ONLY` search with the extracted date.
    *   **Semantic + Filter (Feature 1):** If the query asks about a topic within a timeframe or category (e.g., "Work thoughts last month"), the Router outputs `HYBRID` along with the extracted category (`work`) and date range (`start_date`, `end_date`).
3. **Execution:**
    *   For `SQL_ONLY` (Feature 8): We bypass the vector database completely and use Spring Data JPA (`DailyUserNoteRepository.findByNoteDate(...)`) to fetch the exact raw notes.
    *   For `HYBRID` (Feature 1): We use the Spring AI `VectorStore` but apply a `FilterExpression`. We tell pgvector: "Only search for vectors where `category == 'work'` AND `date >= X and date <= Y`, then find the most semantically similar ones."

---

## 2. Long-Term Summarization (Hierarchical Map-Reduce)

### The Concept
Summarizing an entire year by passing 365 days of notes into an LLM will exceed token limits and cause hallucinations. Instead, we summarize the summaries.

### How it Works (The Process)
1. **Cron Jobs (Spring `@Scheduled`):** We set up scheduled tasks in Spring Boot.
2. **Daily Rollup:** Every night at 11:59 PM, fetch all `DailyUserNote` entries for that day. Ask the LLM to generate a `DailyNoteSummary` (Highs, Lows, Themes) and save it to the database.
3. **Weekly Rollup:** Every Sunday night, fetch the 7 `DailyNoteSummary` records. Ask the LLM to summarize the *week* based on those 7 summaries. Store this in a new entity (`WeeklyNoteSummary`).
4. **Monthly/Yearly Rollup:** Repeat the pattern. At the end of the month, summarize the weeks. At the end of the year, summarize the months.
5. **Retrieval:** When asked "How was 2024?", we simply fetch the single `YearlyNoteSummary` or the 12 `MonthlyNoteSummary` texts. It's fast, cheap, and highly accurate.

---

## 3. Entity Extraction

### The Concept
Turning unstructured text (diary entries) into structured SQL data (people, places, moods) so we can run exact analytics on our life.

### How it Works (The Process)
1. **Post-Ingestion Processing:** After a user saves a note, we trigger an asynchronous event (using Spring `@Async` or Spring Events).
2. **Structured Output LLM Call:** We send the raw note to the LLM with strict instructions to return a JSON object (using Spring AI's `BeanOutputConverter`).
    *   *Prompt:* "Extract all people, locations, and the overall mood (1-10) from this text."
    *   *Output:* `{"people": ["John", "Mom"], "locations": ["Central Park"], "mood": 8}`
3. **Database Storage:** We create relational tables (`NoteEntity`, `NotePerson`, `NoteLocation`) and link them to the `DailyUserNote`. This allows SQL queries like: `SELECT AVG(mood) FROM notes WHERE person = 'John'`.

---

## 4. Automated "On This Day"

### The Concept
A classic journaling feature that surfaces past memories exactly 1, 2, or 5 years ago to spark reflection.

### How it Works (The Process)
1. **API Endpoint:** Create a GET endpoint `/api/v1/notes/on-this-day`.
2. **Database Query:** The service calculates exact dates in the past (e.g., `LocalDate.now().minusYears(1)`).
3. **Fetch & Return:** It uses standard Spring Data JPA to query the `DailyUserNoteRepository` for those specific dates and returns them to the frontend to be displayed as a "Flashback" or "Memory".

---

## 5. Proactive Journaling (Interrogation Mode)

### The Concept
If you don't know what to write, the AI acts as an interviewer, asking you follow-up questions based on your recent life events.

### How it Works (The Process)
1. **Trigger:** The user opens the diary app, but the text box is empty. The frontend calls a `/api/v1/notes/prompt-me` endpoint.
2. **Context Retrieval:** The backend fetches the last 3-5 days of `DailyNoteSummary` to understand the recent context.
3. **LLM Generation:** The LLM is given a prompt: "The user is about to write their diary. Here is what happened recently. Generate 1 or 2 engaging, open-ended questions to help them reflect today."
4. **Delivery:** The question (e.g., "Did you end up finishing that presentation for work?") is displayed as a placeholder or floating prompt in the UI.

---

## 6. Voice Ingestion

### The Concept
Allowing users to speak their diary entries instead of typing them.

### How it Works (The Process)
1. **Frontend Capture:** The UI records audio from the microphone and sends an audio file (e.g., `.wav` or `.webm`) via a `multipart/form-data` POST request.
2. **Audio Transcription:** The Spring Boot backend receives the file and sends it to a Speech-to-Text model (like OpenAI Whisper API, which is supported by Spring AI).
3. **Pipeline Injection:** The transcribed text is then fed directly into the existing `NoteIngestionService` just like a typed note.

---

## 7. GraphRAG (Advanced)

### The Concept
Standard RAG chunks text and loses relationship context. GraphRAG builds a Knowledge Graph (nodes and edges) to understand relationships (e.g., "Meet" -> [Lives In] -> "New York").

### How it Works (The Process)
*Note: This is the most complex feature and requires a graph database (like Neo4j).*
1. **Information Extraction:** Similar to Entity Extraction, but we ask the LLM to extract **Triplets** (Subject, Predicate, Object). Example: `("I", "worked on", "AI Diary Project")`.
2. **Graph Storage:** We store these triplets in a Graph Database.
3. **Graph Traversal Retrieval:** When querying, we first search the Graph DB to find connected concepts, retrieve that massive context web, and feed *that* to the LLM to generate an answer.
*(For V1 of this app, we may want to simulate this heavily using relational Entity Extraction (Feature 3) before fully committing to Neo4j).*
