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
| `description` | `String` | **ADDED** — chatbot config description at time of indexing |
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
  "description": "string",
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
| `description` | **ADDED** | Chatbot config description — allows FastAPI to have context during indexing |

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
  "attemptsMade": "integer | null"
}
```

### New
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

| Field | Change | Notes |
|---|---|---|
| `description` | **ADDED** | YouTube channel description fetched by FastAPI. When `status = COMPLETED`, Spring Boot rewrites `ChatbotConfig.description` with this value and strips the influencer's verification token from it. If `null`, the config description is left untouched. |

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
  "talk_like_me": "boolean",
  "tone": "string | null",
  "verbosity": "string",
  "formality": "string | null"
}
```

### New — `ConfigDTO`
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

| Field | Change | Notes |
|---|---|---|
| `description` | **ADDED** to `config` | Chatbot config description passed on every chat request so the AI can use it as persona/context |

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
| `description` | `String \| null` | Chatbot config description — context for the AI |
| `questions` | `List<QuestionDTO>` | All USER messages with intent `CONTENT_QUESTION` |
| `questions[].text` | `String` | Message content |
| `questions[].answeredWithSources` | `Boolean \| null` | Whether the bot replied with cited video sources |

FastAPI uses this payload to compute: topic clusters, executive summary, and content gaps.

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
  "contentGaps": { }
}
```

| Field | Type | Notes |
|---|---|---|
| `mostAskedClusters` | `JsonNode` (free-form JSON) | Topic clusters with frequency — shape defined by FastAPI |
| `executiveSummary` | `String` | Human-readable paragraph summarising user behaviour |
| `contentGaps` | `JsonNode` (free-form JSON) | Topics users asked about that have no matching video content — shape defined by FastAPI |

These three fields are persisted as `jsonb` columns on `AnalyticsReport` and forwarded as-is to the frontend.
