Issues

1> Search is working but then not accurately I'll have to check things Also chunk size is 1000 i guess it can loose context and it is loosing context.
things to do
- The issue is what about category how is it working
- The entity extracter use

How should I design my front end should it contain category personal or work in different interfaces or is it like ill write my note and then AI will figure it out accordingly.

The llm output must not be too long

- Can I replace llm with a local model so i wont need internet and API CALLS;

- This data is being stored in sql right i need to be shown in front end as well.

- with output i need the place from which it is picked up.

- Also deleting the note should remove vector embedding as well.








inject user notes working
prompt me not tested
inject audio not working

Implement the cognitive twin extension to extract personal heuristics and mental models into PostgreSQL alongside vector search.
Upgrade retrieval to combine semantic chunks with cognitive axioms, paving the way for a future QLoRA local model.

Standard vector RAG fails on deep pattern queries because similarity search cannot trace longitudinal trends across isolated chunks.
Instead, route these reflection queries to your pre-computed monthly and yearly summaries via SQL, fitting months of condensed history directly into the context window.
The LLM can then identify recurring friction points across time and query lower-tier weekly notes only to fetch concrete dates and proof.

I neeed to implemet a system which knows the context of all my conversation before replying it is impossible to know everything but import things cankept in mind and in memory. so that LLM GENERATES AN ACCURATE ANSWERS.

Summary of What ChangesApproachWhat HappensResultCurrent (topK=1)Fetches 1 chunk; LLM sees a single isolated paragraph.Blind spots, incomplete answers.Increased topK + Parent ChunksFetches 5–10 chunks and expands to full-day entries.Complete localized context around the topic.Hierarchical Macro/Micro RAGInjects monthly summaries (broad picture) + vector hits (specific proof).The AI truly acts like it knows your entire life story.
