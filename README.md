# Deterministic Instant Payment Simulator (DIPS)

Web-first, simulation-only UPI-inspired push payment platform with:
- deterministic transaction FSM and append-only event timeline
- issuer/acquirer virtual ledgers with debit/credit/reversal handling
- offline encrypted SMS ingestion (AES-GCM)
- amount-based priority scheduler
- admin failure injection
- live WebSocket streams for payer/payee sequence visualization

## Run Locally (Docker)

```bash
docker compose up --build
```

This compose setup now uses PostgreSQL 17 (works with 17.4).
If you previously ran with PostgreSQL 16, recreate the DB volume once:

```bash
docker compose down -v
docker compose up --build
```

Open:
- `http://localhost:8080/` payer console
- `http://localhost:8080/payee.html` payee console
- `http://localhost:8080/admin.html` admin console

## Run Locally (Maven)

Prereqs: Java 21, Maven.

```bash
cd backend
mvn spring-boot:run
```

For local PostgreSQL (including 17.4), the app uses schema `dips_app` by default
to avoid `public` schema permission issues. Override with:

```bash
set DIPS_DB_SCHEMA=public
```

For local runs, the app now picks a free HTTP port automatically.
Check startup logs for `Tomcat started on port(s): ...` and open that port.
To force a fixed port, set:

```bash
set SERVER_PORT=8080
```

If PostgreSQL is not running locally on `localhost:5432`, start only postgres via docker first:

```bash
docker compose up postgres
```

## APIs

- `POST /api/payments/push`
- `GET /api/payments/{txId}`
- `GET /api/payments/{txId}/events`
- `POST /api/offline/sms/submit`
- `POST /api/admin/failure-scenarios/{scenario}/enable|disable`
- `POST /api/admin/load-profile`
- `GET /api/admin/status`
- `WS /ws/transactions/{txId}`
- `WS /ws/users/{userId}`

## Sample Push Request

```json
{
  "clientRequestId": "demo-req-001",
  "payerVpa": "payer@issuer",
  "payeeVpa": "payee@acquirer",
  "amount": 120.00,
  "mpin": "1111"
}
```

## Initial Virtual Accounts

- `payer@issuer` => 5000.00
- `payee@acquirer` => 1000.00
- `payer2@issuer` => 2500.00
- `payee2@acquirer` => 300.00

## Notes

- This implementation is simulation-only and does not integrate real UPI rails or real money.
- V1 scope is push flow; collect/QR are deferred.
- Module-to-code traceability is in `MODULES.md`.
