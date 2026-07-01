# Replime Backend — Load / Performance Test (JMeter)

Test plan for `replime-backend-springboot`. Base URL assumed: `http://localhost:8080/api/v1` (context path `/api/v1` is set in `application.properties`; DB is Postgres, queue is RabbitMQ per `docker-compose.yml`).

## Files

- `replime-load-test.jmx` — the JMeter test plan (open with JMeter 5.6+, or run headless).
- `load-users.csv` — 50 regular `USER` accounts used for the login/auth thread group.
- `admin-users.csv` — 1 `ADMIN` account.
- `influencer-users.csv` — 10 placeholder `INFLUENCER` accounts (see prerequisite below — not auto-created).
- `seed-test-users.sh` — idempotent curl script that registers the accounts above via `/auth/signup` and `/auth/signup/admin`.

## What's covered

| Thread Group | Endpoints | Auth |
|---|---|---|
| 1 - Public Browsing | `GET /chatbots`, `GET /chatbots/{id}`, `GET /chatbot/categories`, `GET /chatbot/categories/{id}/message-classes` | none (public) |
| 2 - Auth + Logged-in Users | `POST /auth/login` | JWT (extracted from login response, not exercised further — `/auth/loggedin` was a throwaway test endpoint and is excluded) |
| 3 - Admin Flow | `POST /auth/login`, `GET /users` | JWT, role `ADMIN` |
| 4 - Influencer Chatbot Flow (disabled by default) | `POST /auth/login`, `GET /influencer/chatbot`, `/status`, `/message-classes`, `/videos` | JWT, role `INFLUENCER` |

Each authenticated flow logs in fresh per iteration (rather than reusing one static token) so the test also measures login throughput — including bcrypt hashing cost, which is usually the biggest CPU cost in the auth path.

Thread Group 2 only calls `POST /auth/login` — `GET /auth/loggedin` was a throwaway endpoint used to sanity-check the controller during development, not a real business endpoint, so it's excluded from the load test.

### Important prerequisite: the Influencer thread group is disabled

Signup always creates a `USER`. The only path to `INFLUENCER` is `POST /influencer/verify/request` + `/confirm`, which calls the **real YouTube Data API** for a real channel (see `InfluencerVerificationService`). That's not something to automate in a load test — it would burn your YouTube API quota and depends on live external data.

To enable Thread Group 4:
1. Manually verify 1+ real test accounts through the actual app/UI (or have a teammate with DB access promote existing test users to `INFLUENCER` and attach a chatbot).
2. Put their real email/password into `influencer-users.csv`.
3. Right-click the thread group in JMeter and enable it (or set `enabled="true"` in the `.jmx`).

Everything else (thread groups 1–3) works out of the box against a fresh environment.

## Setup

1. Start the app and its dependencies (`docker-compose up -d` for Postgres/RabbitMQ, then run the Spring Boot app).
2. Seed test accounts (safe to re-run — skips accounts that already exist):
   - **PowerShell (Windows, no extra install):**
     ```powershell
     .\seed-test-users.ps1 -BaseUrl "http://localhost:8080/api/v1"
     ```
     If you get an execution-policy error, run this once first in the same window:
     ```powershell
     Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
     ```
   - **Git Bash / WSL / macOS / Linux:**
     ```bash
     chmod +x seed-test-users.sh
     ./seed-test-users.sh http://localhost:8080/api/v1
     ```
3. Install JMeter 5.6+ if you don't have it: https://jmeter.apache.org/download_jmeter.cgi

## Running

All load parameters are overridable with `-J` (defaults shown):

| Property | Default | Meaning |
|---|---|---|
| `HOST` | `localhost` | target host |
| `PORT` | `8080` | target port |
| `PROTOCOL` | `http` | http/https |
| `PUBLIC_THREADS` | 50 | concurrent public browsers |
| `AUTH_THREADS` | 30 | concurrent logged-in users |
| `ADMIN_THREADS` | 3 | concurrent admins |
| `INFLUENCER_THREADS` | 10 | concurrent influencers (only if enabled) |
| `RAMPUP` | 60 | seconds to ramp up to full thread count |
| `DURATION` | 600 | seconds the test runs after ramp-up starts |

### Quick GUI run (for building confidence in the script, not for real numbers)

```bash
jmeter -t replime-load-test.jmx
```

### Headless run (use this for actual capacity numbers — the GUI adds overhead)

```bash
jmeter -n -t replime-load-test.jmx \
  -JHOST=localhost -JPORT=8080 \
  -JPUBLIC_THREADS=100 -JAUTH_THREADS=50 -JADMIN_THREADS=3 \
  -JRAMPUP=60 -JDURATION=600 \
  -l results.jtl -e -o report/
```

This produces `results.jtl` (raw samples) and an HTML dashboard in `report/` (open `report/index.html`).

## Suggested capacity-test scenarios

Run the same plan three ways to build a full picture, per standard load/soak/spike practice:

1. **Ramp-up / baseline** — `RAMPUP=120 DURATION=600` at your expected normal peak concurrency. Confirms the system handles everyday load with acceptable latency and no errors.
2. **Soak** — same thread counts, `DURATION=3600` or longer. Watches for memory leaks, connection pool exhaustion, or slow degradation over time (check JVM heap and Postgres connection count over the run, not just JMeter's own numbers).
3. **Spike / stress** — push thread counts well above expected peak (e.g. 3-5x) with a short `RAMPUP` (e.g. 10-15s) to see where the system starts shedding load or erroring, and how gracefully it recovers afterward. Increase `PUBLIC_THREADS`/`AUTH_THREADS` in steps between runs until error rate climbs, to find the actual ceiling.

Vanilla JMeter ramps threads linearly and can't do true staged/stepped patterns or non-linear spikes on its own. For finer control (step thread groups, throughput shaping timers to hold an exact requests/sec target), install the free **JMeter Plugins Manager** (https://jmeter-plugins.org/) and add "jpgc - Standard Set" — swap the plain `ThreadGroup` elements for `Concurrency Thread Group` / `Throughput Shaping Timer` from that pack if you need that precision.

## Interpreting results

- **Error rate** — should stay near 0% at baseline load; watch which endpoint fails first as you push load (usually `/auth/login` due to bcrypt CPU cost, or DB-backed list endpoints under connection pool pressure).
- **p95/p99 latency** — more informative than average for user-facing SLAs. The Aggregate Report / HTML dashboard both report percentiles.
- **Throughput ceiling** — the requests/sec point where latency stops being flat and starts climbing with more threads — that's your practical capacity, not the point where errors start.
- **Where to look server-side while a run is active**: Postgres active connections and slow query log (`spring.jpa.show-sql=true` is already on, so watch for N+1 patterns — `hibernate.generate_statistics=true` is also enabled, check the app logs for query counts per request), JVM heap/GC via `actuator` if enabled, RabbitMQ queue depth if training-source ingestion is exercised, and the external FastAPI/YouTube dependencies — those are out-of-process calls (`WebClientConfig`) and will bottleneck your own service if they're slow, independent of Spring Boot's own capacity.

## Known scope limits of this plan

- No coverage yet for `POST /influencer/chatbot/training-sources` (video ingestion via RabbitMQ + FastAPI) or the chat/session flow — the session DTOs (`CreateSessionRequestDTO`, `SendMessageResponseDTO`, etc.) exist but no REST controller was found for them, so that flow is likely WebSocket/STOMP-based (`WebSocketConfig`, `/ws/**`). Load-testing WebSocket traffic needs a JMeter WebSocket sampler plugin and a look at the actual `@MessageMapping` handlers — happy to build that as a follow-up if you want end-to-end chat load tested too.
- Write-heavy admin operations (create/delete categories, message classes) are intentionally excluded from the default plan to avoid mutating shared test data during a load run; add them behind a low-percentage Throughput Controller if you want to include writes.
