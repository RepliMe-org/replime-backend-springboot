# Frontend DTOs — Controller Responses & Requests

These DTOs are returned by or accepted by Spring Boot controllers. They form the contract between the backend and the frontend/mobile client.

---

## `AnalyticsReportResponseDTO`

**Exposed by:** `AnalyticsController`  
**Endpoints:**
- `POST /influencer/chatbot/analytics` — generate a new report
- `GET  /influencer/chatbot/analytics/latest` — get the latest report
- `GET  /influencer/chatbot/analytics/report?generatedAt=<ISO_DATETIME>` — get a specific report by timestamp

**Auth:** `INFLUENCER` role required (`Bearer` token). Chatbot is resolved from the JWT — no chatbot ID parameter needed.

### Old
```json
{
  "id": 1,
  "generatedAt": "2026-06-21T16:00:00",
  "periodStart": "2026-06-20T16:00:00",
  "periodEnd": "2026-06-21T16:00:00",
  "classificationBreakdown": [ ... ],
  "mostAskedClusters": { },
  "executiveSummary": "string",
  "contentGaps": { },
  "mostCitedVideos": [ ... ]
}
```

### New
```json
{
  "id": 1,
  "generatedAt": "2026-06-21T16:00:00",
  "generatedAtHistory": [
    "2026-06-21T16:00:00",
    "2026-06-20T14:00:00"
  ],
  "contentGapCountHistory": [5, 3],
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
  "mostCitedVideos": [
    {
      "videoId": "dQw4w9WgXcQ",
      "title": "My Best Editing Tutorial",
      "count": 22
    }
  ]
}
```

| Field | Type | Source | Strategy | Notes |
|---|---|---|---|---|
| `id` | `Long` | DB | — | Auto-generated |
| `generatedAt` | `LocalDateTime` | Server time | — | Timestamp of this specific report |
| `generatedAtHistory` | `List<LocalDateTime>` | DB | — | All report timestamps for this chatbot, newest first. Use to build navigation or a date picker. |
| `contentGapCountHistory` | `List<Integer>` | DB | — | Content gap count for each report, same order as `generatedAtHistory`. Use to render a trend graph over time. |
| `classificationBreakdown` | `List<ClassificationCount>` | Spring Boot | **Cumulative** | All-time proportions. `percentage` rounded to 2 dp. |
| `mostAskedClusters` | `JsonNode` (free-form) | FastAPI | **Incremental** | Topic clusters from messages since the previous report |
| `executiveSummary` | `String` | FastAPI | **Incremental** | Summary of the period's user activity |
| `mostCitedVideos` | `List<CitedVideo>` | Spring Boot | **Cumulative** | All-time citation counts across all bot replies |

> `contentGaps` is **not** in this response — fetch it separately via `GET /content-gaps?generatedAt=...`

**Rate limit:** `POST /influencer/chatbot/analytics` enforces a 1-minute cooldown per chatbot (testing value; will increase to 24 hours for production). If called too soon, returns HTTP `429`:
```json
{
  "timestamp": "2026-06-21T16:00:00",
  "success": false,
  "error": "Analytics can only be generated once every 1 minute(s)",
  "nextAvailableAt": "2026-06-21T16:01:00"
}
```

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

## `ContentGapResponseDTO` — **NEW**

**Exposed by:** `AnalyticsController`  
**Endpoint:** `GET /influencer/chatbot/analytics/content-gaps?generatedAt=<ISO_DATETIME>`

**Auth:** `INFLUENCER` role required (`Bearer` token).

### Old
Did not exist (content gaps were embedded in `AnalyticsReportResponseDTO`).

### New
```json
{
  "generatedAt": "2026-06-21T16:00:00",
  "contentGaps": [ ]
}
```

| Field | Type | Notes |
|---|---|---|
| `generatedAt` | `LocalDateTime` | Timestamp of the report these gaps belong to |
| `contentGaps` | `JsonNode` (JSON array) | Topics users asked about that have no matching video. Shape defined by FastAPI. Empty array if none. |

Use the `generatedAtHistory` from `AnalyticsReportResponseDTO` to know which timestamps are available, then fetch individual gap lists as needed.

---

## No Changes to Existing Frontend DTOs

| DTO | Used by | Status |
|---|---|---|
| `SendMessageResponseDTO` | `POST /sessions/{sessionId}/messages` | Unchanged |
| `MessageDto` | Nested in `SendMessageResponseDTO` and `GET /sessions/{sessionId}/messages` | Unchanged |
| `SessionResponseDTO` | `POST /sessions`, `GET /sessions/{sessionId}` | Unchanged |
| `SessionListResponseDTO` | `GET /sessions` | Unchanged |
| `VideoResponseDTO` | `GET /influencer/chatbot/videos` | Unchanged |
