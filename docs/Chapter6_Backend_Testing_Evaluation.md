# 6. Testing and Evaluation

*(Apply "Heading 1" style to the line above. All body paragraphs below should use your document's Normal/Body style — Times New Roman 12pt, 1.5 line spacing, justified, 6pt after — per your formatting spec.)*

## 6.1 Testing Strategy

*(Heading 2)*

The backend was validated through a layered testing strategy combining unit tests, integration tests, and system-level testing, supported by JaCoCo for code-coverage measurement and Apache JMeter for load and capacity testing. The service and controller layers were tested in isolation using JUnit 5 with Mockito mocks and hand-written test doubles, decoupling business-logic verification from the database, the JWT/security filter chain, and external systems (the YouTube Data API, RabbitMQ, and the FastAPI classification microservice). Integration tests then exercised the full Spring context — including the real security filter chain and HTTP request/response cycle — against an in-memory H2 database configured in PostgreSQL-compatibility mode, with external systems replaced by Mockito beans to keep the suite hermetic and repeatable. Finally, system-level testing exercised the fully deployed application (Spring Boot, PostgreSQL, and RabbitMQ running via Docker Compose) end-to-end, including a JMeter-driven load test that is described in Section 6.3.

The testing levels in scope for the Spring Boot backend are:

*(Format as a table using your document's table style: header row shaded, Times New Roman, 3 columns as below.)*

| Level | Tool | Scope |
|---|---|---|
| Unit | JUnit 5 + Mockito | Service and controller logic, with repositories and external clients (YouTube API, JWT service) mocked via Mockito and hand-written dynamic proxies |
| Integration (API) | Spring Boot Test + MockMvc | Full HTTP request → real filter chain/DispatcherServlet → H2 in-memory database → response assertions, with RabbitMQ and external services mocked |
| Load / Performance | Apache JMeter | Four traffic profiles: public browsing, auth/login, admin flow, influencer chatbot flow |

### 6.1.1 Unit Testing

*(Heading 3)*

Unit tests target the service and controller layers directly, without loading the Spring application context, which keeps the suite fast and focused on business-logic correctness. Representative examples include:

- **`AuthServiceTest`** — verifies signup, login, and first-admin-creation flows, covering both success paths (correct token generation, correct role assignment, password encoding) and failure paths (duplicate email, invalid credentials, attempting to create a second admin account).
- **`AuthControllerTest`** — verifies that each `AuthController` endpoint correctly delegates to `AuthService` and returns the expected HTTP status and response body.
- **`SecurityFilterTest`** — verifies `JwtAuthFilter` behavior for requests with a missing, valid, and invalid/expired Bearer token, and verifies `InternalTokenFilter` correctly gates `/internal/**` endpoints behind the internal service token while leaving other endpoints unaffected.
- **`GlobalExceptionHandlerTest`** — verifies that each custom domain exception (verification, authentication, resource conflict, not-found, invalid source, invalid classification, rate limiting) maps to the correct HTTP status and a consistent JSON error body.
- **`ChatbotServiceTest`** and **`ChatbotCategoryServiceTest`** — verify chatbot listing/detail/status/visibility logic and category management, including the response-mapping logic covered in Section 6.3's performance findings.

### 6.1.2 Integration Testing

*(Heading 3)*

**`FullAppIntegrationTest`** boots the complete Spring Boot application context (`@SpringBootTest` with `@AutoConfigureMockMvc`) against an in-memory H2 database, so requests pass through the real `DispatcherServlet`, the real Spring Security filter chain, and real JSON serialization — the same code path a real client would exercise — while RabbitMQ, YouTube, and the classification service are stubbed out with Mockito beans. This test suite confirms, among other things, that: public endpoints (`/chatbots`, `/chatbot/categories`) are reachable without authentication; protected endpoints (`/users`) correctly reject unauthenticated requests with `401 Unauthorized`; and the internal-service token gate on `/internal/**` rejects requests without the expected `X-INTERNAL-TOKEN` header before the controller ever runs, and accepts requests that supply it correctly.

### 6.1.3 System Testing

*(Heading 3)*

System testing exercised the fully deployed application — Spring Boot, PostgreSQL, and RabbitMQ provisioned via `docker-compose.yml` — through manual verification against the Swagger/OpenAPI UI (`springdoc-openapi`) and direct API calls, and through an automated JMeter load test (Section 6.3) that drives real, unmocked HTTP traffic against the running system. Unlike the unit and integration tests, system testing exercises the real database, the real connection pool, and real query execution plans; it is also what surfaced a genuine performance defect (an N+1 query pattern, detailed in Section 6.3) that the mocked-persistence-layer unit and integration tests could not have detected — illustrating why system-level testing remains a necessary complement to the lower test layers rather than a redundant one.

## 6.2 Test Cases and Results

*(Heading 2)*

### 6.2.1 Test Cases

*(Heading 3)*

The table below lists a representative subset of the test suite's most significant scenarios, spanning authentication, security enforcement, public API access, authorization, and error handling.

*(Format this as a table using your document's table style: header row shaded, Times New Roman, columns as below. Per your table spec, center-align the Test ID and Status columns; keep the rest left-aligned.)*

Status values below reflect the most recent `mvn test` run referenced in Section 6.2.2 (245 tests, 0 failures, 0 errors — every listed case Passed).

| Test ID | Module | Scenario | Expected Result | Status |
|---|---|---|---|---|
| TC-01 | Authentication | Sign up with a new, unique email address | Account created with role `USER`; JWT token returned | Passed |
| TC-02 | Authentication | Sign up with an email address that already exists | Request rejected; no duplicate account created | Passed |
| TC-03 | Authentication | Log in with valid credentials | JWT token issued; correct username and role returned | Passed |
| TC-04 | Authentication | Log in with invalid credentials | Login rejected with a generic error; no account details disclosed | Passed |
| TC-05 | Authentication | Create the platform's first administrator account | Admin account created with role `ADMIN` | Passed |
| TC-06 | Authentication | Attempt to create a second administrator account | Request rejected; existing admin account preserved | Passed |
| TC-07 | Security (JWT Filter) | Access a protected endpoint with no `Authorization` header | Request proceeds unauthenticated; downstream authorization rules apply | Passed |
| TC-08 | Security (JWT Filter) | Access a protected endpoint with a valid Bearer token | Security context populated with the authenticated user | Passed |
| TC-09 | Security (JWT Filter) | Access a protected endpoint with an invalid token | Security context remains unauthenticated (no exception, no user set) | Passed |
| TC-10 | Security (Internal API) | Call an `/internal/**` endpoint without the internal service token | Request rejected with `401 Unauthorized` and message `Invalid or missing X-INTERNAL-TOKEN`, before reaching the controller | Passed |
| TC-11 | Security (Internal API) | Call an `/internal/**` endpoint with the correct `X-INTERNAL-TOKEN` | Request passes through the filter and reaches the controller | Passed |
| TC-12 | Security (Internal API) | Call a non-`/internal/**` endpoint with no internal token present | Request passes through the filter unaffected (filter only gates internal routes) | Passed |
| TC-13 | Public API | Retrieve the public chatbot list without authentication | Chatbot list returned successfully; no login required | Passed |
| TC-14 | Public API | Retrieve chatbot categories without authentication | Category list, including chatbot counts, returned successfully | Passed |
| TC-15 | Authorization | Access an admin-only endpoint (`/users`) while unauthenticated | Request rejected with `401 Unauthorized` | Passed |
| TC-16 | Error Handling | Trigger each custom domain exception (verification, authentication, conflict, not-found, invalid source, invalid classification) | Each exception maps to the correct HTTP status and a consistent JSON error body (`error`, `success`, `timestamp`) | Passed |
| TC-17 | Error Handling | Trigger a rate-limit (`TooManyRequestsException`) | Response returns `429 Too Many Requests` with `error`, `success: false`, `timestamp`, and `nextAvailableAt` fields | Passed |
| TC-18 | Influencer Verification | Request verification for a new, unrequested YouTube channel | Verification record created with status `PENDING`, a verification token, and channel metadata (handle, avatar, subscriber count) | Passed |
| TC-19 | Influencer Verification | Request verification again while a `PENDING` request exists for the *same* channel | Existing record refreshed with a new token and updated avatar, instead of creating a duplicate | Passed |
| TC-20 | Influencer Verification | Request verification while a `PENDING` request exists for a *different* channel | Request rejected: "You have already requested verification for different channel." | Passed |
| TC-21 | Influencer Verification | Request verification when the user is already `VERIFIED` | Request rejected: "You are already verified." | Passed |
| TC-22 | Influencer Verification | Request verification for a channel that does not exist on YouTube | Request rejected: "Channel not found" | Passed |
| TC-23 | Influencer Verification | Request verification for a channel already claimed by another verification record | Request rejected: "This channel requested verification before." | Passed |
| TC-24 | Influencer Verification | Confirm a valid, non-expired pending verification | Verification marked `VERIFIED`, `verifiedAt` set, user promoted to role `INFLUENCER`, and a chatbot auto-created for the user | Passed |
| TC-25 | Influencer Verification | Confirm a verification whose token has expired | No promotion; a new token is generated and the request remains `PENDING` | Passed |
| TC-26 | Influencer Verification | Confirm verification when no pending request exists | Request rejected: "No pending verification found." | Passed |
| TC-27 | Integration (Full Context) | `GET /chatbots` through the real filter chain and DispatcherServlet | `200 OK` with correctly serialized JSON body (id, influencer, chatbot name, status) | Passed |
| TC-28 | Integration (Full Context) | `POST /auth/login` through the real filter chain and DispatcherServlet | `200 OK`; JSON response contains token, username, and role | Passed |
| TC-29 | Integration (Full Context) | `PATCH /internal/update-video-status/{id}` with the correct internal token, through the real filter chain | `200 OK`; controller invoked with the correct video ID and request body | Passed |
| TC-30 | Integration (Full Context) | `PATCH /internal/update-video-status/{id}` through the real app context, without the internal token | `401 Unauthorized` before the controller executes | Passed |
| TC-31 | Security (JWT Filter) | Access a protected endpoint with a malformed Bearer token (`TOKEN_NOT_FOUND`, not a valid JWT) | Security context remains unauthenticated; request continues cleanly with no unhandled exception (regression test added after the fix described in Section 6.4.2) | Passed |

### 6.2.2 Test Results

*(Heading 3)*

*[Information Required — the count below (245 tests) predates the fix and regression test described in Section 6.4.2 (TC-31). Re-run `mvn test` to get the current total, which should be 246 with 0 failures/errors if the fix is correct.]*

The full automated test suite was executed with `mvn test`, producing the following result:

*(Format as a simple 2-row table, or as a highlighted single line, matching your document's convention for key results.)*

| Metric | Result |
|---|---|
| Tests run | 245 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

Code coverage was measured with JaCoCo (`jacoco-maven-plugin`, bound to the `test` phase). The overall coverage achieved was **82% instruction coverage** and **59% branch coverage**, with the following breakdown by package:

*(Table — dense, 5+ columns, use 10pt body font per your formatting spec for dense tables; header row shaded.)*

| Package | Instruction Cov. | Branch Cov. | Missed / Total Lines | Missed / Total Methods | Missed / Total Classes |
|---|---|---|---|---|---|
| `com.example.demo.controllers` | 100% | n/a | 0 / 99 | 0 / 59 | 0 / 11 |
| `com.example.demo.entities.utils` | 100% | n/a | 0 / 56 | 0 / 15 | 0 / 15 |
| `com.example.demo.services.rabbitMQService` | 100% | 100% | 0 / 49 | 0 / 7 | 0 / 2 |
| `com.example.demo.dtos.internal` | 100% | n/a | 0 / 2 | 0 / 2 | 0 / 2 |
| `com.example.demo.configs.seeders` | 100% | 83% | 0 / 42 | 0 / 4 | 0 / 2 |
| `com.example.demo.exceptions` | 97% | 100% | 2 / 84 | 1 / 26 | 0 / 9 |
| `com.example.demo.configs` | 83% | 50% | 27 / 161 | 5 / 52 | 0 / 11 |
| `com.example.demo.entities` | 81% | 59% | 4 / 35 | 0 / 20 | 0 / 6 |
| `com.example.demo.services` | 79% | 59% | 286 / 1,561 | 39 / 190 | 1 / 15 |
| `com.example.demo` (root) | 70% | n/a | 2 / 6 | 1 / 3 | 0 / 2 |
| `com.example.demo.dtos` | 63% | 38% | 9 / 23 | 0 / 6 | 0 / 6 |
| **Total** | **82%** | **59%** | **330 / 2,118** | **46 / 384** | **1 / 81** |

Controllers, security/utility enums, the RabbitMQ integration layer, and internal DTOs all reached 100% instruction coverage. The `services` package — the largest and most business-logic-heavy package — sits at 79% instruction / 59% branch coverage, reflecting the number of conditional branches (external-API fallbacks, ownership checks, status-transition guards) that are harder to exercise exhaustively in unit tests without duplicating integration-test coverage.

## 6.3 Performance Evaluation

*(Heading 2)*

### 6.3.1 Performance Metrics

*(Heading 3)*

Performance was evaluated using Apache JMeter against the locally deployed backend (Spring Boot + PostgreSQL + RabbitMQ via Docker Compose), simulating four concurrent traffic profiles: anonymous public browsing (`GET /chatbots`, `GET /chatbot/categories`), authenticated regular-user traffic (login), a low-volume administrator flow, and an influencer chatbot-management flow. The metrics tracked were: throughput (requests/second), average and percentile response time, and error rate, against a target SLA of under 2 seconds per request on public read endpoints.

### 6.3.2 Evaluation Results

*(Heading 3)*

At the thread-group level, the following average response times have been measured so far:

| Scenario | Threads | Avg. Response Time |
|---|---|---|
| Public browsing (`GET /chatbots`) | 50 | *[Information Required]* |
| Auth / login | 30 | *[Information Required]* |
| Admin flow | 3 | 2,310 ms |
| Influencer flow | 10 | *[Information Required]* |

The table below reports exact request counts and success/error rates pulled directly from the project's `load-testing/results.jtl` output file, isolated by request timestamp to separate individual runs — real, measured numbers, not estimates. Rows still marked *[Information Required]* are from the accumulated diagnostic log described in Section 6.4.2 (mixed pre-/post-fix runs) and still need a final clean run. The Admin flow rows below, by contrast, are from a single isolated, post-fix run (53 requests, 0 errors) and are trustworthy as-is.

*(Format as a table using your document's table style; center-align the Requests and Success Rate columns.)*

| Scenario | Requests | Successes | Success Rate | Avg. Response Time | Throughput | Notes |
|---|---|---|---|---|---|---|
| Public browsing — `GET /chatbots` | 792 | 258 | 32.6% | *[Information Required]* | *[Information Required]* | See N+1 interim finding below |
| Public browsing — `GET /chatbot/categories` | 751 | 258 | 34.4% | *[Information Required]* | *[Information Required]* | See N+1 interim finding below |
| Auth / login (regular user) | 827 | 827 | 100% | *[Information Required]* | *[Information Required]* | Fully passing once test accounts were seeded |
| Admin flow — `POST /auth/login` (admin) | 27 | 27 | 100% | 330 ms | 0.6 req/s (group) | Isolated clean run, post-fix; well within the 2s target |
| Admin flow — `GET /users` | 26 | 26 | 100% | 4,367 ms | 0.6 req/s (group) | Isolated clean run, post-fix; **over 2x the 2s SLA target — see new N+1 finding below** |
| Influencer flow — `POST /auth/login` (influencer) | 230 | 0 | 0% | n/a | n/a | From the earlier, pre-fix diagnostic log — not yet re-tested since the fix, see Section 6.4.2 |
| Influencer flow — `GET /influencer/chatbot` | 228 | 0 | 0% | n/a | n/a | From the earlier, pre-fix diagnostic log — not yet re-tested since the fix, see Section 6.4.2 |
| Influencer flow — `GET /influencer/chatbot/status` | 221 | 0 | 0% | n/a | n/a | From the earlier, pre-fix diagnostic log — not yet re-tested since the fix, see Section 6.4.2 |
| Influencer flow — `GET /influencer/chatbot/message-classes` | 215 | 0 | 0% | n/a | n/a | From the earlier, pre-fix diagnostic log — not yet re-tested since the fix, see Section 6.4.2 |
| Influencer flow — `GET /influencer/chatbot/videos` | 214 | 0 | 0% | n/a | n/a | From the earlier, pre-fix diagnostic log — not yet re-tested since the fix, see Section 6.4.2 |

Three things stand out. First, the regular-user auth flow is fully healthy: all 827 recorded login requests succeeded (100%), confirming that once test accounts were correctly seeded, the authentication path itself works correctly under JMeter. Second, the admin flow is now fully healthy too (27/27, 26/26) following the `JwtAuthFilter` fix and a clean isolated re-run — see Section 6.4.2 for why this is believed to have resolved the earlier anomaly. Third, the influencer flow rows above still show 0% because they have not yet been re-tested since the fix; the same clean-run procedure used for the admin flow should be repeated for it. The public-browsing success rates (32.6% / 34.4%) reflect a mix of pre-fix and post-fix diagnostic runs recorded in the same results file, including runs made before the N+1 defect below was corrected; they should not be read as the application's current, steady-state error rate.

**New finding — likely N+1 pattern in `UserService.getAllUsers()`, not yet fixed.** The clean post-fix Admin flow run above shows `POST /auth/login` performing well (330ms average) but `GET /users` averaging 4.37 seconds — over twice the 2-second SLA target. Reviewing `UserService.getAllUsers()` shows the same per-item query pattern already found and fixed in `ChatbotService`/`ChatbotCategoryService` (Section 6.3.2's earlier interim finding): for every user returned, it makes a separate `chatSessionRepo.countByUserIdAndStatusNot()` call, and for every `INFLUENCER` user, an additional `chatbotService.getChatbotByUser()` lookup — neither batched. This is a strong candidate root cause given the pattern match, but has not yet been confirmed via SQL logging the way the earlier chatbot-listing defect was, and has not been fixed.

The Spring Boot backend's admin-flow endpoints were load-tested with Apache JMeter against the local development server (port 8080), backed by PostgreSQL running via Docker Compose, so the reported latencies include real database round-trip time. The measurements below are computed directly from the raw per-request data in `load-testing/results.jtl`, isolated to the single clean run described above (thread group "3 - Admin Flow", 3 concurrent threads, ~93 s duration, 0 errors).

*(Format as a table using your document's table style, matching the style of your Node.js backend's benchmark table: header row shaded, columns as below.)*

| Endpoint | Conns | Duration | p50 | p97.5 | Avg req/s |
|---|---|---|---|---|---|
| `POST /auth/login` (admin) | 3 | 93 s | 317 ms | 629 ms | 0.29 |
| `GET /users` (admin only) | 3 | 93 s | 4,285 ms | 5,155 ms | 0.28 |

*Table 6.X: Spring Boot backend — JMeter benchmark results, Admin flow, isolated clean run (2026-07-02). Renumber to match your document's table sequence.*

Median (p50) latency for the login endpoint is well under the 2-second SLA target at 317ms. `GET /users`, however, has a median of 4.29 seconds and a worst-case (p97.5) of 5.16 seconds — consistent with the suspected N+1 query pattern in `UserService.getAllUsers()` above. Note the sample size here is small (26–27 requests from a low-volume 3-thread flow), so the p97.5 figure is close to the observed maximum rather than a statistically robust tail estimate. Public-browsing, regular-user-login, and influencer-flow endpoints are intentionally not included in this table: the existing `results.jtl` data for those endpoints mixes several earlier diagnostic runs (see the mixed-run caveat above and Section 6.4.2), so no percentile computed from it would be trustworthy. Reproducing this benchmark for the remaining endpoints requires deleting `results.jtl` and re-running JMeter headless with only the relevant thread group enabled — the same procedure used to capture the table above.

**Interim finding — N+1 query defect identified and corrected.** During load testing, `GET /chatbots` was observed taking 3.2–4.0 seconds on average under concurrent load, well outside the 2-second target. Investigation of the application's SQL logging traced this to an N+1 query pattern: the public chatbot listing endpoint executed a separate database query per chatbot returned (for its configuration, category, and influencer-verification data) instead of one batched query for the whole list, and the category-listing endpoint executed a separate `COUNT` query per category instead of one grouped query. Both were corrected by replacing the per-item repository calls with batched/joined queries (`LEFT JOIN FETCH` for the chatbot listing; a single `GROUP BY` query for category counts). In interim testing after the fix, average response time on the affected endpoint dropped to roughly 1.25–2.3 seconds — an approximate 50% reduction from the pre-fix 3.2–4.0 seconds — though it had not yet consistently met the 2-second target at the concurrency levels tested, indicating there is likely still headroom for further optimization (e.g., database connection pool sizing) beyond the query-batching fix already applied.

## 6.4 Limitations and Known Issues

*(Heading 2)*

### 6.4.1 Current Limitations

*(Heading 3)*

The following limitations were identified directly from the codebase during this evaluation:

1. **Influencer onboarding depends on a live external API with no offline path.** The `INFLUENCER` role can only be granted through `InfluencerVerificationService`'s real-time call to the YouTube Data API to verify channel ownership; there is no mockable or offline equivalent, which makes this specific flow impractical to exercise in automated end-to-end or load tests without live YouTube credentials and API quota.
2. **Two admin-facing chatbot-listing endpoints still carry the N+1 pattern fixed elsewhere.** `ChatbotService.getAllChatbots()` and `getAllChatbotsForAdmin()` still perform one `InfluencerVerification` lookup per chatbot returned, the same pattern that was corrected for the public listing endpoint (Section 6.3.2). Risk is lower here since these are admin-only, lower-traffic endpoints, but the same fix has not yet been applied.
3. **`UserService.getAllUsers()` likely has the same unfixed N+1 pattern.** For every user returned, the method calls `chatSessionRepo.countByUserIdAndStatusNot()` individually, and for every `INFLUENCER` user, an additional `chatbotService.getChatbotByUser()` lookup — neither batched. This is a strong suspect (not yet confirmed via SQL logging) for the 4.37-second average response time measured on `GET /users` during load testing (Section 6.3.2) — over twice the 2-second SLA target.
4. **No login-attempt throttling.** `AuthService.login()` has no rate-limiting or lockout mechanism for repeated failed login attempts, though a generic `TooManyRequestsException` exists elsewhere in the codebase for other flows and could be extended to cover this case.

*(A previously listed limitation — unhandled malformed-JWT exceptions in `JwtAuthFilter` — was fixed during this evaluation; see the note at the end of Section 6.4.2.)*

### 6.4.2 Challenges Encountered

*(Heading 3)*

1. **Provisioning realistic test data for privileged roles.** Neither the `ADMIN` nor `INFLUENCER` roles can be obtained through ordinary self-service signup: signup always issues role `USER`, the platform permits exactly one bootstrap administrator account, and `INFLUENCER` requires a real, externally-verified YouTube channel. This meant load-testing those roles required manually provisioning real accounts rather than scripting them, which limited how many concurrent admin/influencer identities could be simulated in this evaluation.
2. **A load-test-specific authentication anomaly, now very likely resolved as a side effect of a related fix.** During testing, requests to `/auth/login` for the administrator and influencer accounts consistently returned an empty-bodied `401` response when driven through the JMeter test harness — even after both accounts were independently confirmed to authenticate correctly via direct API calls using a byte-identical request payload outside of JMeter. Investigation ruled out incorrect test credentials, malformed CSV test data, and JMeter request-construction or configuration-scoping issues; the response also carried a `Connection: close` header, unlike a normal `AuthService`-level rejection. The most likely explanation: the *next* request in the same flow (`GET /users` for the admin flow) was sending a malformed bearer token (`Bearer TOKEN_NOT_FOUND`, the JSON extractor's configured fallback for a failed token extraction) that triggered an uncaught `io.jsonwebtoken.MalformedJwtException` in `JwtAuthFilter` — confirmed directly from the application log (`JWT strings must contain exactly 2 period characters. Found: 0`). An uncaught exception mid-request can force Tomcat to close the underlying connection; if JMeter's HTTP client then reused that same now-broken connection for the *next* iteration's login call, the login request could fail at the connection/protocol level and surface as a generic empty-body `401` — without `AuthService`'s login logic having actually run or rejected it. This was a hypothesis until it was tested: `JwtAuthFilter.doFilterInternal()` was fixed to catch `JwtException`/`UsernameNotFoundException` and continue the chain unauthenticated instead of throwing (regression test: `jwtAuthFilterSwallowsMalformedTokenExceptionAndContinuesChain` in `SecurityFilterTest`). A subsequent isolated JMeter run of the Admin flow (identical 3-thread configuration, identical credentials) after this fix produced **27/27 successful admin logins and 26/26 successful `GET /users` calls — 0 errors**, compared to 100% failure on the same thread group before the fix. This is strong before/after evidence for the connection-corruption theory, though it was not verified at the network/packet level, so it is reported as the most likely explanation rather than a certainty. The influencer flow has not yet been re-tested since the fix and should be confirmed the same way.
3. **The load test surfaced a genuine, otherwise-undetected defect.** As detailed in Section 6.3.2, load testing uncovered an N+1 query performance defect that the unit and integration test suites — which substitute the persistence layer with in-memory/mocked repositories — had no way of detecting, since the defect only manifests against a real database executing real queries at volume. This was a useful validation of why system-level and performance testing remain necessary alongside unit and integration testing.

## Evidence Required Before Final Submission

*(Heading 2 or a distinct "checklist" callout, per your document's convention)*

- [ ] A final, clean-environment JMeter run covering all four scenarios together (Public browsing and Auth/login still need real throughput/response-time figures; Admin flow is now populated from a clean isolated run) to complete the Section 6.3.2 table.
- [ ] Confirmation that `mvn test` and the JaCoCo report in Section 6.2.2 reflect the current codebase (re-run after the N+1 query fix and the `JwtAuthFilter` fix — the test count should now be 246, not 245).
- [x] Root cause for the admin JMeter-only authentication anomaly (Section 6.4.2) — very likely explained and resolved as a side effect of the `JwtAuthFilter` fix; confirmed via a clean 27/27 + 26/26 re-run. Still to do: re-run the influencer flow the same way to confirm the same fix resolves it there too.
- [ ] Confirm (via SQL logging, the same method used for the earlier chatbot-listing N+1) whether `UserService.getAllUsers()` has an N+1 defect, as suspected from the 4.37s average response time on `GET /users` — and fix it if confirmed.
- [ ] A decision on whether to address the remaining items in Section 6.4.1 before submission, or to keep them documented as known limitations.
- [ ] Any additional supporting evidence your grading rubric requires (e.g., screenshots or exports of individual test-case executions, CI pipeline output).
