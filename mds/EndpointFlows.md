# Endpoint Flows: Frontend → Backend → AI
## Every endpoint — flow + request schema + response schema

---

## QUICK REFERENCE

| # | Method | Endpoint | Auth | Hits FastAPI |
|---|--------|----------|------|-------------|
| 1 | POST | `/api/v1/sources` | Influencer JWT | YES (async) |
| 2 | GET | `/api/v1/sources/{source_id}/status` | Influencer JWT | NO |
| 3 | DELETE | `/api/v1/sources/{source_id}` | Influencer JWT | YES (sync) |
| 4 | POST | `/api/v1/chat/sessions` | User JWT | NO |
| 5 | POST | `/api/v1/chat/sessions/{session_id}/messages` | User JWT | YES (sync) |
| 6 | GET | `/api/v1/chat/sessions/{session_id}/messages` | User JWT | NO |
| 7 | POST | `/api/v1/chat/messages/{message_id}/feedback` | User JWT | NO |
| 8 | DELETE | `/api/v1/chat/sessions/{session_id}` | User JWT | NO |
| 9 | GET | `/api/v1/chatbots/{chatbot_id}/config` | Public | NO |
| 10 | PUT | `/api/v1/chatbots/{chatbot_id}/config` | Influencer JWT | NO |
| I1 | POST | `/api/v1/internal/videos/{video_id}/index` | X-Service-Key | FastAPI endpoint |
| I2 | PUT | `/api/v1/internal/videos/{video_id}/status` | X-Service-Key | Backend endpoint |
| I3 | DELETE | `/api/v1/internal/videos/{video_id}` | X-Service-Key | FastAPI endpoint |
| I4 | POST | `/api/v1/chat/process` | X-Service-Key | FastAPI endpoint |

---

## FLOW 1 — Add Training Source

**`POST /api/v1/sources`**

### Request
```json
{
  "chatbot_id": "uuid",
  "source_type": "VIDEO",
  "source_value": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
}
```
`source_type`: `"VIDEO"` | `"PLAYLIST"` | `"LAST_N"`
`source_value`: full URL for VIDEO/PLAYLIST, number string `"10"` for LAST_N

### Flow
```
FRONTEND
  │
  │  POST /api/v1/sources
  │
  ▼
BACKEND
  ├─ 1. Validate JWT → extract influencer_id
  ├─ 2. Verify chatbot belongs to this influencer
  ├─ 3. Extract youtube_video_id from URL
  ├─ 4. Call YouTube Data API v3
  │       GET /videos?id={youtube_video_id}&part=snippet,contentDetails
  │       ← title, thumbnail_url, duration (seconds), channelId
  │
  ├─ 5. Ownership check
  │       video.channelId == influencer.youtube_channel_id
  │       NO → 403 Forbidden
  │
  ├─ 6. INSERT training_source
  │       (chatbot_id, source_type, source_value, sync_status=PROCESSING)
  │
  ├─ 7. INSERT video
  │       (source_id, youtube_video_id, title,
  │        thumbnail_url, duration, sync_status=PROCESSING)
  │
  ├─ 8. POST http://ai-service/api/v1/internal/videos/{video_id}/index
  │       { chatbot_id, youtube_video_id }
  │       ← FastAPI returns 202 immediately
  │         background task started — backend does NOT wait
  │
  └─ 9. Return 202 to frontend

FASTAPI (background — no blocking)
  ├─ 1. Fetch transcript   (YouTube Captions → LangChain fallback)
  ├─ 2. Chunk transcript   (RecursiveCharacterTextSplitter, 400 tokens, 50 overlap)
  ├─ 3. Embed chunks       (paraphrase-multilingual-MiniLM-L12-v2, 384-dim)
  ├─ 4. Upsert ChromaDB    (collection: "chatbot_{chatbot_id}")
  │                         (chunk IDs: "chunk_{video_id}_{index}")
  └─ 5. PUT /internal/videos/{video_id}/status
         { sync_status: "COMPLETED" }   ← or "FAILED" + error_message

BACKEND (on FastAPI callback — Flow I2)
  ├─ UPDATE video SET sync_status=COMPLETED, processed_at=NOW()
  └─ If all videos for source are COMPLETED:
       UPDATE training_source SET sync_status=COMPLETED
```

### Response `202 Accepted`
```json
{
  "source_id": "uuid",
  "video_id": "uuid",
  "youtube_video_id": "dQw4w9WgXcQ",
  "title": "ML Fundamentals",
  "thumbnail_url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
  "status": "PROCESSING"
}
```

### Error Responses
```json
400 { "error": "INVALID_URL",      "message": "Could not extract video ID from URL" }
400 { "error": "VIDEO_NOT_FOUND",  "message": "YouTube video does not exist" }
403 { "error": "NOT_YOUR_VIDEO",   "message": "This video does not belong to your channel" }
409 { "error": "ALREADY_INGESTED", "message": "This video is already in your knowledge base" }
```

---

## FLOW 2 — Check Video Sync Status

**`GET /api/v1/sources/{source_id}/status`**
*(Frontend polls this every 3 seconds after Flow 1)*

### Flow
```
FRONTEND  (polls every 3s, stops when COMPLETED or FAILED)
  │
  │  GET /api/v1/sources/{source_id}/status
  │
  ▼
BACKEND
  ├─ 1. Validate JWT
  ├─ 2. Verify source belongs to influencer's chatbot
  ├─ 3. SELECT training_source + video WHERE source_id = ?
  └─ 4. Return 200

  No FastAPI call. Reads PostgreSQL only.
```

### Response `200 OK`
```json
{
  "source_id": "uuid",
  "sync_status": "PROCESSING",
  "video": {
    "video_id": "uuid",
    "title": "ML Fundamentals",
    "thumbnail_url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
    "duration": 1200,
    "sync_status": "PROCESSING",
    "processed_at": null
  }
}
```

`sync_status`: `"PROCESSING"` | `"COMPLETED"` | `"FAILED"`

When `"FAILED"`:
```json
{
  "source_id": "uuid",
  "sync_status": "FAILED",
  "video": {
    "video_id": "uuid",
    "sync_status": "FAILED",
    "error_message": "Could not retrieve transcript: video has no captions"
  }
}
```

---

## FLOW 3 — Delete Video

**`DELETE /api/v1/sources/{source_id}`**

### Flow
```
FRONTEND
  │
  │  DELETE /api/v1/sources/{source_id}
  │
  ▼
BACKEND
  ├─ 1. Validate JWT → verify influencer owns chatbot → owns source
  ├─ 2. SELECT video WHERE source_id = ?  → video_id, chatbot_id
  │
  ├─ 3. DELETE http://ai-service/api/v1/internal/videos/{video_id}
  │       { chatbot_id }
  │       ← backend waits (sync)
  │       ← FastAPI returns { deleted_chunks: 47 }
  │
  ├─ 4. DELETE video WHERE id = ?
  ├─ 5. DELETE training_source WHERE id = ?
  └─ 6. Return 204

FASTAPI (sync — backend waits)
  ├─ 1. Query all chunk IDs WHERE metadata.video_id = video_id
  ├─ 2. collection.delete(ids=[...])
  └─ 3. Return { deleted_chunks: 47 }
```

### Response `204 No Content`

### Error Responses
```json
404 { "error": "SOURCE_NOT_FOUND", "message": "Source does not exist" }
403 { "error": "FORBIDDEN",        "message": "You do not own this source" }
```

---

## FLOW 4 — Create Chat Session

**`POST /api/v1/chat/sessions`**

### Request
```json
{
  "chatbot_id": "uuid"
}
```

### Flow
```
FRONTEND
  │
  │  POST /api/v1/chat/sessions
  │
  ▼
BACKEND
  ├─ 1. Validate JWT → extract user_id
  ├─ 2. Verify chatbot exists
  ├─ 3. INSERT chat_session (chatbot_id, user_id, started_at)
  ├─ 4. SELECT chatbot_config WHERE chatbot_id = ?
  └─ 5. Return 201

  No FastAPI call.
```

### Response `201 Created`
```json
{
  "session_id": "uuid",
  "chatbot_id": "uuid",
  "chatbot_name": "Python Pro AI",
  "tone": "friendly",
  "started_at": "2024-04-16T10:00:00Z"
}
```

---

## FLOW 5 — Send Message (Main RAG Flow)

**`POST /api/v1/chat/sessions/{session_id}/messages`**

### Request
```json
{
  "content": "How do I train a neural network?",
  "language": "en"
}
```
`language`: optional — FastAPI auto-detects if omitted

### Flow
```
FRONTEND
  │
  │  POST /api/v1/chat/sessions/{session_id}/messages
  │
  ▼
BACKEND
  ├─ 1. Validate JWT → extract user_id
  ├─ 2. Verify session belongs to this user
  ├─ 3. Validate content (not empty, max 5000 chars)
  │
  ├─ 4. INSERT message (USER)
  │       { session_id, content, sender=USER, language, sent_at }
  │       → user_message_id
  │
  ├─ 5. SELECT last 10 messages WHERE session_id = ?
  │       ORDER BY sent_at DESC LIMIT 10
  │
  ├─ 6. SELECT chatbot_config WHERE chatbot_id = ?
  │
  ├─ 7. POST http://ai-service/api/v1/chat/process   ← WAIT (sync)
  │       ← Response in ~1.5-2.5 seconds
  │
  ├─ 8. INSERT message (BOT)
  │       { session_id, content=answer, sender=BOT,
  │         sources, retrieval_ms, llm_ms, rewritten_query, sent_at }
  │       → bot_message_id
  │
  ├─ 9. UPDATE chat_session
  │       SET message_count = message_count + 2,
  │           last_message_at = NOW()
  │
  └─ 10. Return 201

FASTAPI (sync — see Flow I4 for full RAG pipeline detail)
```

### Response `201 Created`
```json
{
  "user_message_id": "uuid",
  "bot_message_id": "uuid",
  "answer": "To train a neural network, you need to start with your data...",
  "sources": [
    {
      "video_id": "uuid",
      "video_title": "ML Fundamentals",
      "chunk_text": "Training a neural network involves a forward pass...",
      "youtube_url": "https://youtube.com/watch?v=xyz&t=120s",
      "timestamp_seconds": 120,
      "similarity_score": 0.89
    }
  ],
  "metadata": {
    "retrieval_ms": 45,
    "llm_ms": 1240,
    "query_rewritten": false,
    "original_query": null
  },
  "sent_at": "2024-04-16T10:00:05Z"
}
```

Fallback — no relevant chunks:
```json
{
  "user_message_id": "uuid",
  "bot_message_id": "uuid",
  "answer": "I don't have information about that in this chatbot's content. Try asking about machine learning, neural networks, or Python instead.",
  "sources": [],
  "metadata": {
    "retrieval_ms": 30,
    "llm_ms": 0,
    "query_rewritten": false,
    "original_query": null
  },
  "sent_at": "2024-04-16T10:00:05Z"
}
```

### Error Responses
```json
400 { "error": "EMPTY_CONTENT",     "message": "Message cannot be empty" }
400 { "error": "CONTENT_TOO_LONG",  "message": "Message cannot exceed 5000 characters" }
404 { "error": "SESSION_NOT_FOUND", "message": "Session does not exist" }
403 { "error": "FORBIDDEN",         "message": "You do not own this session" }
503 { "error": "AI_UNAVAILABLE",    "message": "AI service is temporarily unavailable" }
```

---

## FLOW 6 — Get Chat History

**`GET /api/v1/chat/sessions/{session_id}/messages`**

### Query Parameters
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `limit` | int | 50 | Max messages to return |
| `offset` | int | 0 | Skip N messages |

### Flow
```
FRONTEND
  │
  │  GET /api/v1/chat/sessions/{session_id}/messages?limit=50&offset=0
  │
  ▼
BACKEND
  ├─ 1. Validate JWT
  ├─ 2. Verify session belongs to this user
  ├─ 3. SELECT message WHERE session_id = ?
  │       ORDER BY sent_at ASC  LIMIT ? OFFSET ?
  └─ 4. Return 200

  No FastAPI call.
```

### Response `200 OK`
```json
{
  "session_id": "uuid",
  "messages": [
    {
      "id": "uuid",
      "sender": "USER",
      "content": "What is deep learning?",
      "language": "en",
      "sent_at": "2024-04-16T10:00:00Z"
    },
    {
      "id": "uuid",
      "sender": "BOT",
      "content": "Deep learning is a subset of machine learning...",
      "sources": [
        {
          "video_id": "uuid",
          "video_title": "ML Basics",
          "youtube_url": "https://youtube.com/watch?v=xyz&t=120s",
          "timestamp_seconds": 120,
          "similarity_score": 0.87
        }
      ],
      "user_feedback": null,
      "sent_at": "2024-04-16T10:00:05Z"
    }
  ],
  "total_count": 24,
  "limit": 50,
  "offset": 0,
  "has_more": false
}
```

---

## FLOW 7 — Rate a Response

**`POST /api/v1/chat/messages/{message_id}/feedback`**

### Request
```json
{
  "feedback": 1
}
```
`feedback`: `-1` downvote | `0` neutral | `1` upvote

### Flow
```
FRONTEND
  │
  │  POST /api/v1/chat/messages/{message_id}/feedback
  │
  ▼
BACKEND
  ├─ 1. Validate JWT
  ├─ 2. SELECT message WHERE id = ?
  ├─ 3. Verify sender = BOT
  ├─ 4. Verify message's session belongs to this user
  ├─ 5. Validate feedback ∈ { -1, 0, 1 }
  ├─ 6. UPDATE message SET user_feedback = ?
  └─ 7. Return 200

  No FastAPI call.
```

### Response `200 OK`
```json
{
  "message_id": "uuid",
  "feedback": 1,
  "updated_at": "2024-04-16T10:00:10Z"
}
```

---

## FLOW 8 — Delete Chat Session

**`DELETE /api/v1/chat/sessions/{session_id}`**

### Flow
```
FRONTEND
  │
  │  DELETE /api/v1/chat/sessions/{session_id}
  │
  ▼
BACKEND
  ├─ 1. Validate JWT
  ├─ 2. Verify session belongs to this user
  ├─ 3. DELETE message WHERE session_id = ?   (FK cascade)
  ├─ 4. DELETE chat_session WHERE id = ?
  └─ 5. Return 204

  No FastAPI call. Chroma has no session-level data.
```

### Response `204 No Content`

---

## FLOW 9 — Get Chatbot Config

**`GET /api/v1/chatbots/{chatbot_id}/config`**

### Flow
```
FRONTEND
  │
  │  GET /api/v1/chatbots/{chatbot_id}/config
  │
  ▼
BACKEND
  ├─ 1. No auth required (public read)
  ├─ 2. SELECT chatbot_config WHERE chatbot_id = ?
  └─ 3. Return 200

  No FastAPI call.
```

### Response `200 OK`
```json
{
  "chatbot_id": "uuid",
  "chatbot_name": "Python Pro AI",
  "tone": "friendly",
  "persona_description": "You are a friendly Python tutor who loves using real-world analogies...",
  "persona_keywords": ["pythonic", "clean code", "beginner-friendly"],
  "response_length": "detailed",
  "top_k": 5,
  "similarity_threshold": 0.7,
  "max_context_turns": 10,
  "language": "en"
}
```

---

## FLOW 10 — Update Chatbot Config

**`PUT /api/v1/chatbots/{chatbot_id}/config`**

### Request
```json
{
  "chatbot_name": "ML Expert",
  "tone": "professional",
  "persona_description": "You are a rigorous ML researcher who cites sources precisely...",
  "persona_keywords": ["rigorous", "evidence-based", "precise"],
  "response_length": "detailed"
}
```
All fields optional — only provided fields are updated.

### Flow
```
FRONTEND
  │
  │  PUT /api/v1/chatbots/{chatbot_id}/config
  │
  ▼
BACKEND
  ├─ 1. Validate JWT → verify influencer owns this chatbot
  ├─ 2. UPDATE chatbot_config SET ... WHERE chatbot_id = ?
  └─ 3. Return 200

  No FastAPI call.
  FastAPI reads the updated config on the next /chat/process call.
```

### Response `200 OK`
```json
{
  "chatbot_id": "uuid",
  "chatbot_name": "ML Expert",
  "tone": "professional",
  "persona_description": "You are a rigorous ML researcher...",
  "persona_keywords": ["rigorous", "evidence-based", "precise"],
  "response_length": "detailed",
  "top_k": 5,
  "similarity_threshold": 0.7,
  "max_context_turns": 10,
  "language": "en",
  "updated_at": "2024-04-16T10:00:00Z"
}
```

---

## INTERNAL FLOW I1 — Index Video (Backend → FastAPI)

**`POST /api/v1/internal/videos/{video_id}/index`**

### Request
```
Header: X-Service-Key: {shared_secret}
```
```json
{
  "chatbot_id": "uuid",
  "youtube_video_id": "dQw4w9WgXcQ"
}
```

### FastAPI Pattern
```python
@app.post("/api/v1/internal/videos/{video_id}/index")
async def index_video(video_id: str, request: IndexRequest, background_tasks: BackgroundTasks):
    background_tasks.add_task(
        run_ingestion_pipeline,
        video_id=video_id,
        chatbot_id=request.chatbot_id,
        youtube_video_id=request.youtube_video_id
    )
    return { "status": "accepted", "video_id": video_id }

async def run_ingestion_pipeline(video_id, chatbot_id, youtube_video_id):
    try:
        transcript  = await transcription_service.get(youtube_video_id)
        chunks      = await chunking_service.chunk(transcript)
        embeddings  = await embedding_service.embed(chunks)
        await chroma_service.upsert(chatbot_id, video_id, embeddings)
        await notify_backend(video_id, "COMPLETED")
    except Exception as e:
        await notify_backend(video_id, "FAILED", error=str(e))
```

### Response `202 Accepted`
```json
{
  "status": "accepted",
  "video_id": "uuid"
}
```

---

## INTERNAL FLOW I2 — FastAPI Reports Status (FastAPI → Backend)

**`PUT /api/v1/internal/videos/{video_id}/status`**

### Request
```
Header: X-Service-Key: {shared_secret}
```
```json
{
  "sync_status": "COMPLETED",
  "error_message": null
}
```

On failure:
```json
{
  "sync_status": "FAILED",
  "error_message": "No transcript available: video has no captions"
}
```

### Backend Logic
```
FASTAPI calls Backend
  │
  ▼
BACKEND
  ├─ 1. Validate X-Service-Key
  ├─ 2. UPDATE video
  │       SET sync_status = ?, processed_at = NOW()
  │       WHERE id = ?
  └─ 3. If COMPLETED:
           SELECT COUNT(*) FROM video
           WHERE source_id = this_video.source_id
             AND sync_status != 'COMPLETED'
           IF count = 0:
             UPDATE training_source SET sync_status = 'COMPLETED'
```

### Response `200 OK`
```json
{ "acknowledged": true }
```

---

## INTERNAL FLOW I3 — Delete Video Chunks (Backend → FastAPI)

**`DELETE /api/v1/internal/videos/{video_id}`**

### Request
```
Header: X-Service-Key: {shared_secret}
```
```json
{
  "chatbot_id": "uuid"
}
```

### FastAPI Logic
```
FASTAPI (sync — backend waits)
  ├─ 1. Get collection "chatbot_{chatbot_id}"
  ├─ 2. Query all IDs WHERE metadata.video_id = video_id
  ├─ 3. collection.delete(ids=[...])
  └─ 4. Return { deleted_chunks: N }
```

### Response `200 OK`
```json
{
  "video_id": "uuid",
  "deleted_chunks": 47
}
```

---

## INTERNAL FLOW I4 — Process Chat (Backend → FastAPI)

**`POST /api/v1/chat/process`**

### Request
```
Header: X-Service-Key: {shared_secret}
```
```json
{
  "session_id": "uuid",
  "chatbot_id": "uuid",
  "query": "How do I train a neural network?",
  "language": "en",
  "conversation_history": [
    { "role": "USER", "content": "What is deep learning?",  "sent_at": "2024-04-16T10:00:00Z" },
    { "role": "BOT",  "content": "Deep learning is...",     "sent_at": "2024-04-16T10:00:05Z" }
  ],
  "config": {
    "chatbot_name": "Python Pro AI",
    "persona_description": "You are a friendly Python tutor...",
    "persona_keywords": ["pythonic", "beginner-friendly"],
    "tone": "friendly",
    "response_length": "detailed",
    "top_k": 5,
    "similarity_threshold": 0.7,
    "max_context_turns": 10
  }
}
```

### FastAPI RAG Pipeline
```
  ├─ 1. Preprocess query
  │       lowercase, strip spaces
  │       detect pronouns: it, this, that, they, them
  │
  ├─ 2. Rewrite if pronouns found  (Groq, 50 tokens, temp=0.1)
  │       "How do I implement it?" → "How do I implement gradient descent?"
  │       skip if no pronouns
  │
  ├─ 3. Embed query
  │       paraphrase-multilingual-MiniLM-L12-v2 → 384-dim vector
  │
  ├─ 4. Search ChromaDB
  │       collection: "chatbot_{chatbot_id}"
  │       n_results: top_k,  filter: similarity >= threshold
  │
  ├─ 5. No chunks above threshold?
  │       → return fallback immediately (skip 6–8)
  │
  ├─ 6. Build weighted context
  │       reverse conversation_history → chronological
  │       weight = 0.5 ^ (position_from_end / 3)
  │
  ├─ 7. Build prompt
  │       persona + tone + keywords + context + chunks + query + instructions
  │
  ├─ 8. Call Groq
  │       model: llama-3.1-8b-instant
  │       max_tokens: 1024,  temperature: 0.7
  │
  └─ 9. Return to backend
```

### Response `200 OK`
```json
{
  "answer": "To train a neural network, think of it like teaching a student through examples. As covered in [ML Fundamentals](https://youtube.com/watch?v=xyz&t=120s) at 2:00...",
  "sources": [
    {
      "video_id": "uuid",
      "video_title": "ML Fundamentals",
      "chunk_text": "Training a neural network involves a forward pass...",
      "youtube_url": "https://youtube.com/watch?v=xyz&t=120s",
      "timestamp_seconds": 120,
      "similarity_score": 0.89
    }
  ],
  "retrieval_ms": 45,
  "llm_ms": 1240,
  "rewritten_query": null
}
```

If query was rewritten:
```json
{
  "answer": "...",
  "sources": [...],
  "retrieval_ms": 45,
  "llm_ms": 1240,
  "rewritten_query": "How do I implement gradient descent?"
}
```

Fallback — no relevant chunks:
```json
{
  "answer": "I don't have information about that in Python Pro AI's content. Try asking about Python loops, functions, or object-oriented programming.",
  "sources": [],
  "retrieval_ms": 30,
  "llm_ms": 0,
  "rewritten_query": null
}
```

---

## KEY RULES

**FastAPI is called only in 3 situations:**
1. Video submitted → index pipeline (async, BackgroundTasks)
2. User sends message → RAG pipeline (sync, backend waits ~2s)
3. Video deleted → Chroma cleanup (sync, backend waits ~200ms)

**PostgreSQL is the single source of truth for:**
- Video sync status — frontend polls this, never FastAPI directly
- All chat history
- All chatbot configuration

**ChromaDB is owned exclusively by FastAPI:**
- Spring Boot never reads or writes Chroma
- All chunk data lives here — not in PostgreSQL
