# Internal DTOs — FastAPI Communication

These DTOs are in `dtos/internal/` and are used exclusively for communication between Spring Boot and FastAPI (HTTP or RabbitMQ). They are never exposed directly to the frontend.

---

## `VideoIndexMessage` — RabbitMQ → FastAPI worker

**Direction:** Spring Boot publishes → FastAPI worker consumes  
**Transport:** RabbitMQ

### Old
| Field | Type | Notes |
|---|---|---|
| `youtubeVideoId` | `String` | |
| `chatbotId` | `String` | |
| `videoTitle` | `String` | |
| `description` | `String` | Chatbot config description at time of indexing |
| `trainingSourceId` | `Long` | |
| `idempotencyKey` | `String` | Format: `video:{id}:attempt:{n}` |
| `attemptNumber` | `Integer` | |
| `startFromStage` | `String` | |
| `publishedAt` | `String` | ISO offset datetime |

### New
| Field | Type | Notes |
|---|---|---|
| `youtubeVideoId` | `String` | |
| `chatbotId` | `String` | |
| `videoTitle` | `String` | |
| ~~`description`~~ | ~~`String`~~ | **REMOVED** — FastAPI now generates the description itself during ingestion; it does not need it from Spring Boot |
| `trainingSourceId` | `Long` | |
| `idempotencyKey` | `String` | Format: `video:{id}:attempt:{n}` |
| `attemptNumber` | `Integer` | |
| `startFromStage` | `String` | |
| `publishedAt` | `String` | ISO offset datetime |

---

## `VideoIndexRequestDTO` — Spring Boot → FastAPI `POST /ingest/videos`

**Direction:** Spring Boot sends → FastAPI receives  
**Transport:** HTTP (WebClient)

### Old
```json
{
  "chatbot_id": "string",
  "description": "string",
  "videos": [
    {
      "youtube_video_id": "string",
      "video_title": "string"
    }
  ]
}
```

### New
```json
{
  "chatbot_id": "string",
  "videos": [
    {
      "youtube_video_id": "string",
      "video_title": "string"
    }
  ]
}
```

| Field | Change | Notes |
|---|---|---|
| ~~`description`~~ | **REMOVED** | FastAPI generates `aiGeneratedDescription` from video content during ingestion — no input description needed |

---

## `UpdateVideoStatusRequestDTO` — FastAPI → Spring Boot `PATCH /internal/update-video-status/{youtubeVideoId}`

**Direction:** FastAPI webhook → Spring Boot receives  
**Transport:** HTTP

### Old
```json
{
  "status": "string",
  "failedStage": "string | null",
  "failureReason": "string | null",
  "retryable": "boolean | null",
  "attemptsMade": "integer | null",
  "description": "string | null"
}
```

### New
_Shape unchanged._ Behaviour of the `description` field changed:

| Field | Change | Notes |
|---|---|---|
| `description` | **Behaviour changed** | FastAPI-generated description of the chatbot content. When `status = COMPLETED`, Spring Boot saves this to `ChatbotConfig.aiGeneratedDescription` (not `description`). No token stripping. The user's manually written `ChatbotConfig.description` is never touched. |

**`status` values:** `COMPLETED` · `FAILED` · `DEAD`

---

## `BotQueryRequestDTO` — Spring Boot → FastAPI `POST /chat/process`

**Direction:** Spring Boot sends → FastAPI receives  
**Transport:** HTTP (WebClient)

Only the nested `ConfigDTO` changed; the top-level shape is unchanged.

### Old — `ConfigDTO`
```json
{
  "chatbot_name": "string",
  "description": "string",
  "talk_like_me": "boolean",
  "tone": "string | null",
  "verbosity": "string",
  "formality": "string | null"
}
```

### New — `ConfigDTO`
_Shape unchanged._ Source of `description` changed:

| Field | Change | Notes |
|---|---|---|
| `description` | **Source changed** | Now populated from `ChatbotConfig.aiGeneratedDescription` (FastAPI-generated). Was previously populated from `ChatbotConfig.description` (user-written). Will be `null` until the first COMPLETED ingestion for this chatbot. |

**Full request shape (unchanged fields shown for reference):**
```json
{
  "chatbot_id": "string",
  "message_id": "long | null",
  "query": "string",
  "conversation_history": [{ "role": "string", "content": "string" }],
  "first_message": "boolean",
  "message_classes": [{ "id": "long", "name": "string" }],
  "config": { ... }
}
```

---

## `BotQueryResponseDTO` — FastAPI → Spring Boot `POST /chat/process` response

**Direction:** FastAPI replies → Spring Boot reads  
**Transport:** HTTP (WebClient)

### Old
```json
{
  "answer": "string",
  "sources": [
    {
      "video_id": "string",
      "video_title": "string",
      "youtube_url": "string"
    }
  ],
  "session_title": "string | null"
}
```

### New
```json
{
  "answer": "string",
  "sources": [
    {
      "video_id": "string",
      "video_title": "string",
      "youtube_url": "string"
    }
  ],
  "session_title": "string | null",
  "intent": "string | null",
  "message_id": "long | null"
}
```

| Field | Change | Notes |
|---|---|---|
| `intent` | **ADDED** | Message intent classified by FastAPI. Possible values: `GREETING` · `SMALL_TALK` · `CONTENT_QUESTION` · `OUT_OF_SCOPE` · `HARMFUL`. Stored on the USER `Message` entity as `MessageIntent` enum. Unknown values are logged and ignored. |
| `message_id` | **ADDED** | The user message ID echoed back by FastAPI so Spring Boot can match the classification to the correct `Message` row. |

---

## `AnalyticsProcessRequestDTO` — **NEW** — Spring Boot → FastAPI `POST /analytics/process`

**Direction:** Spring Boot sends → FastAPI receives  
**Transport:** HTTP (WebClient)

### Old
Did not exist.

### New
```json
{
  "chatbotId": "string",
  "description": "string | null",
  "questions": [
    {
      "text": "string",
      "answeredWithSources": "boolean | null"
    }
  ]
}
```

| Field | Type | Notes |
|---|---|---|
| `chatbotId` | `String` | UUID of the chatbot |
| `description` | `String \| null` | `ChatbotConfig.aiGeneratedDescription` — FastAPI-generated context. `null` until first COMPLETED ingestion. |
| `questions` | `List<QuestionDTO>` | **Incremental** — only USER `CONTENT_QUESTION` messages since the previous report's `generatedAt` (or epoch `2000-01-01` for the first report). Used by FastAPI for topic clusters, executive summary, and content gaps. |
| `questions[].text` | `String` | Message content |
| `questions[].answeredWithSources` | `Boolean \| null` | Whether the bot replied with cited video sources |

FastAPI uses `questions` (the incremental window) to compute topic clusters, executive summary, and content gaps. Classification breakdown and most-cited videos are computed locally by Spring Boot from **all-time** data and are not sent to FastAPI.

---

## `AnalyticsProcessResponseDTO` — **NEW** — FastAPI → Spring Boot `POST /analytics/process` response

**Direction:** FastAPI replies → Spring Boot reads  
**Transport:** HTTP (WebClient)

### Old
Did not exist.

### New
```json
{
  "mostAskedClusters": { },
  "executiveSummary": "string",
  "contentGaps": [ ]
}
```

| Field | Type | Notes |
|---|---|---|
| `mostAskedClusters` | `JsonNode` (free-form JSON) | Topic clusters with frequency — shape defined by FastAPI |
| `executiveSummary` | `String` | Human-readable paragraph summarising user behaviour |
| `contentGaps` | `JsonNode` (**must be a JSON array**) | Topics users asked about that have no matching video content. Spring Boot calls `.size()` on this to compute `contentGapCount` — FastAPI must always return an array (empty array `[]` if none). |
