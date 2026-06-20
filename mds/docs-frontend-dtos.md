# Frontend DTOs — Controller Responses & Requests

These DTOs are returned by or accepted by Spring Boot controllers. They form the contract between the backend and the frontend/mobile client.

---

## `AnalyticsReportResponseDTO` — **NEW**

**Exposed by:** `AnalyticsController`  
**Endpoints:**
- `POST /influencer/chatbot/analytics` — generate report (returns single object)
- `GET  /influencer/chatbot/analytics` — list all reports (returns array)
- `GET  /influencer/chatbot/analytics/latest` — get latest report (returns single object or `null`)

**Auth:** `INFLUENCER` role required (`Bearer` token). Chatbot is resolved from the JWT — no chatbot ID parameter needed.

### Old
Did not exist.

### New
```json
{
  "id": 1,
  "generatedAt": "2026-06-20T18:00:00",
  "classificationBreakdown": [
    {
      "messageClass": "CONTENT_QUESTION",
      "count": 42,
      "percentage": 70.0
    },
    {
      "messageClass": "GREETING",
      "count": 10,
      "percentage": 16.67
    },
    {
      "messageClass": "UNCLASSIFIED",
      "count": 8,
      "percentage": 13.33
    }
  ],
  "mostAskedClusters": { },
  "executiveSummary": "string",
  "contentGaps": { },
  "mostCitedVideos": [
    {
      "videoId": "dQw4w9WgXcQ",
      "title": "My Best Editing Tutorial",
      "count": 22
    }
  ]
}
```

| Field | Type | Source | Notes |
|---|---|---|---|
| `id` | `Long` | DB primary key | Auto-generated on each report generation |
| `generatedAt` | `LocalDateTime` | Server time at generation | |
| `classificationBreakdown` | `List<ClassificationCount>` | Computed locally by Spring Boot | Based on all USER messages with intent `CONTENT_QUESTION`. `messageClass` is the custom class name the influencer assigned, or `"UNCLASSIFIED"`. `percentage` is rounded to 2 decimal places. |
| `mostAskedClusters` | `JsonNode` (free-form) | FastAPI | Topic clusters of frequently asked questions — shape decided by FastAPI |
| `executiveSummary` | `String` | FastAPI | Human-readable paragraph summarising chatbot usage |
| `contentGaps` | `JsonNode` (free-form) | FastAPI | Topics users asked about with no matching video content — shape decided by FastAPI |
| `mostCitedVideos` | `List<CitedVideo>` | Computed locally by Spring Boot | Ranked by how often each video was cited across all bot replies |

#### `ClassificationCount` shape
| Field | Type |
|---|---|
| `messageClass` | `String` |
| `count` | `long` |
| `percentage` | `double` |

#### `CitedVideo` shape
| Field | Type |
|---|---|
| `videoId` | `String` (YouTube video ID) |
| `title` | `String` |
| `count` | `long` |

---

## No Changes to Existing Frontend DTOs

The DTOs below are **unchanged** from the frontend's perspective. Internal behaviour was updated (e.g. `intent` and `answeredWithSources` are now saved on the `Message` entity), but these fields are not yet surfaced in the response shapes.

| DTO | Used by | Status |
|---|---|---|
| `SendMessageResponseDTO` | `POST /sessions/{sessionId}/messages` | Unchanged |
| `MessageDto` | Nested in `SendMessageResponseDTO` and `GET /sessions/{sessionId}/messages` | Unchanged |
| `SessionResponseDTO` | `POST /sessions`, `GET /sessions/{sessionId}` | Unchanged |
| `SessionListResponseDTO` | `GET /sessions` | Unchanged |
| `VideoResponseDTO` | `GET /influencer/chatbot/videos` | Unchanged |
