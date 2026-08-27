# Order Processing Microservices

Two small Spring Boot services that process a stream of order events from a CSV file and maintain a live net position per trading symbol.

- **order-update-service** — reads the CSV, validates each row, and forwards valid events to the other service.
- **position-maintaining-service** — receives events, keeps an in-memory net position per symbol, and exposes it over a REST endpoint.

---

## Architecture & Communication Choices

**Two independent Spring Boot processes**, each independently runnable, communicating over plain HTTP.

**Why HTTP instead of Kafka/gRPC/etc.**
The assignment explicitly allows a simple mechanism, and states that a correct, well-tested solution is preferred over unnecessary infrastructure. HTTP requires no broker, no extra containers, and no additional configuration — `order-update-service` just needs the base URL of `position-maintaining-service`. For a two-service, single-machine exercise like this, HTTP is simpler to run, simpler to test, and simpler to reason about than a message broker.

**Event payload (JSON)**
```json
{
  "eventId": "evt-0001",
  "symbol": "RELIANCE",
  "transactionType": "BUY",
  "quantity": 90
}
```
`order-update-service` builds this object from each valid CSV row and sends it as the body of `POST /events`.

**How delivery errors are surfaced**
Each send is wrapped in a try/catch. If `position-maintaining-service` is unreachable, slow, or returns an error status, `order-update-service` logs the failure (event id + reason) and moves on to the next row — it does not stop or crash. There is no retry queue; a failed send is simply lost for that run.

**Delivery limitations**
- At-most-once delivery — if a POST fails, the event is not retried or persisted anywhere.
- No acknowledgement beyond a 200 response — `order-update-service` doesn't confirm the position was actually applied, only that the HTTP call succeeded.
- If `position-maintaining-service` restarts, its in-memory state (positions + seen event IDs) resets to empty. This is explicitly out of scope per the assignment.

---

## In-Memory State (No Database)

Per the assignment, persistence and durable delivery are explicitly out of scope. Neither service uses a database. `position-maintaining-service` holds:

```java
private final Map<String, Long> positions = new ConcurrentHashMap<>();
private final Set<String> seenEventIds = ConcurrentHashMap.newKeySet();
```

`ConcurrentHashMap` is used (rather than plain `HashMap`/`HashSet`) because `GET /position` and `POST /events` can be invoked concurrently by different threads, and the assignment requires reads and writes to remain correct under that condition.

---

## Event Validation Rules

Applied to every CSV row before it's sent, in this order. The first failing check wins, and the reason is logged:

1. `event_id` must not be null/blank
2. `symbol` must not be null/blank
3. `transaction_type` must be exactly `BUY` or `SELL`
4. `quantity` must parse as an integer, and be present and positive
5. `event_id` must not have already been seen (first valid event with a given id wins; later duplicates are skipped)

Malformed rows (wrong column count, non-numeric quantity, etc.) are logged with a clear reason and skipped — the file continues processing.

`position-maintaining-service` performs its own, independent duplicate check on `event_id` before applying a position update, since it cannot assume every event it receives has already been de-duplicated upstream.

---

## Project Structure

```
order-update-service/
├── src/main/java/com/order_update_service/
│   ├── OrderUpdateServiceApplication.java
│   ├── model/OrderEvent.java
│   ├── service/OrderService.java       # validation + duplicate check
│   └── config/
│       ├── CsvProcessor.java           # reads CSV, sends events over HTTP
│       └── CommandLineConfig.java      # triggers processing on startup
├── order_updates.csv
├── src/test/java/...
└── pom.xml

position-maintaining-service/
├── src/main/java/com/position_maintaining_service/
│   ├── PositionMaintainingServiceApplication.java
│   ├── model/OrderEvent.java
│   ├── service/PositionService.java
│   └── controller/PositionController.java
├── src/test/java/...
└── pom.xml
```

---

## Setup & Run Instructions

**Requirements:** Java 17+, Maven, both projects unzipped/cloned side by side.

### 1. Start `position-maintaining-service` first

```bash
cd position-maintaining-service
mvn spring-boot:run
```

It starts on port `8081` (configurable — see below) and immediately exposes `GET /position` and `POST /events`.

### 2. Start `order-update-service`

```bash
cd order-update-service
mvn spring-boot:run
```

On startup it automatically reads `order_updates.csv` from the project root, validates each row, and posts valid events to `position-maintaining-service`. Progress is printed to the terminal as it runs.

> Order matters: if `order-update-service` starts first, its initial POST calls will fail with a connection error (logged, not fatal) until `position-maintaining-service` comes up.

### 3. Check the result

```bash
curl http://localhost:8081/position
```

```json
{ "RELIANCE": 60, "TCS": -75 }
```

---

## Configuration

Set in each service's `application.properties` (or via environment variables / command-line flags, which Spring Boot honors automatically, e.g. `--order.csv.path=/path/to/file.csv`).

**order-update-service**
| Property | Purpose | Example |
|---|---|---|
| `order.csv.path` | Path to the input CSV | `order_updates.csv` |
| `position.service.url` | Base URL of position-maintaining-service | `http://localhost:8081` |

**position-maintaining-service**
| Property | Purpose | Example |
|---|---|---|
| `server.port` | Port this service listens on | `8081` |

---

## API Usage

### `POST /events`
Accepts a single order event.

```bash
curl -X POST http://localhost:8081/events \
  -H "Content-Type: application/json" \
  -d '{"eventId":"evt-9001","symbol":"INFY","transactionType":"BUY","quantity":25}'
```
Response: `200 OK` (empty body). Duplicate `event_id`s are silently ignored (logged server-side) and still return `200`.

### `GET /position`
Returns the current net position for every symbol seen in an accepted event, including zero and negative positions.

```bash
curl http://localhost:8081/position
```
```json
{ "RELIANCE": 90, "TCS": -75, "INFY": 25 }
```

---

## Running Tests

```bash
# from each service's own directory
mvn test
```

Test coverage includes:
- BUY and SELL position math, including multiple symbols
- Negative and zero net positions
- Duplicate `event_id` handling
- Invalid `transaction_type` values
- Zero, negative, non-integer, and blank quantities
- Blank `event_id` / `symbol`
- Continuing to process subsequent rows after an invalid row
- `GET /position` response shape and values

---

## Known Limitations & Trade-offs

- **No persistence.** Both services hold state purely in memory, by design. Restarting `position-maintaining-service` clears all positions and the duplicate-event tracking.
- **At-most-once delivery.** A failed HTTP POST is logged and dropped — there's no retry or dead-letter mechanism.
- **Throttle is approximate, not exact.** The 50 events/sec cap is implemented with a fixed `Thread.sleep` between sends rather than a precise token-bucket, per the assignment's own guidance that sub-millisecond timing isn't expected.
- **No authentication** on either service's endpoints — out of scope per the assignment.
- **Single-instance only.** Neither service is designed to run more than one instance at a time (no shared state / clustering).

---

## AI-Assisted Tools

Parts of this solution's structure and explanations were developed with the help of an AI assistant (Claude) for guidance on Spring Boot wiring, validation logic, and documentation. All code was reviewed, understood, and can be explained/defended in full.
