# Service API Contracts (Distributed Deterministic Model)

## Scope

This contract freezes behavior between:

1. `PSP -> Switch`
2. `Switch -> Bank`
3. `PSP -> Bank` (status query for timeout resolution)

All operations are idempotent by `transactionId` and must propagate `correlationId`.

## Common Headers

1. `X-Correlation-Id`: global trace identifier across services.
2. `X-Request-Timestamp`: ISO-8601 timestamp at sender.
3. `Content-Type: application/json`

## Common Response Envelope

```json
{
  "transactionId": "uuid",
  "operation": "DEBIT|CREDIT|REVERSAL",
  "status": "SUCCESS|FAILED|NOT_FOUND",
  "reasonCode": "NONE|INSUFFICIENT_FUNDS|ACCOUNT_NOT_FOUND|DUPLICATE|INTERNAL_ERROR",
  "message": "human readable summary",
  "processedAt": "2026-03-08T13:19:52.558+05:30"
}
```

## PSP -> Switch APIs

### `POST /api/switch/debit` (alias: `/switch/debit`)

```json
{
  "transactionId": "uuid",
  "correlationId": "uuid",
  "accountId": "payer@issuer",
  "amount": 100.00
}
```

### `POST /api/switch/credit` (alias: `/switch/credit`)

```json
{
  "transactionId": "uuid",
  "correlationId": "uuid",
  "accountId": "payee@acquirer",
  "amount": 100.00
}
```

### `POST /api/switch/reverse` (alias: `/switch/reverse`)

```json
{
  "transactionId": "uuid",
  "correlationId": "uuid",
  "accountId": "payer@issuer",
  "amount": 100.00
}
```

## Switch -> Bank APIs

Switch forwards to bank-facing endpoints without changing `transactionId` and `correlationId`.

1. `POST /api/bank/debit` (alias: `/bank/debit`)
2. `POST /api/bank/credit` (alias: `/bank/credit`)
3. `POST /api/bank/reverse` (alias: `/bank/reverse`)

Payload shape is equivalent to upstream request.

## PSP -> Bank Status Query

### `GET /api/bank/transaction/{transactionId}?operation=DEBIT|CREDIT|REVERSE`

Compatibility alias supported:

1. `GET /bank/transaction/{transactionId}?operation=...`

Used only for deterministic timeout resolution.

Possible `status` values:

1. `SUCCESS`
2. `FAILED`
3. `NOT_FOUND`

## Timeout and Retry Policy (Deterministic)

1. PSP waits up to `3s` for `debit`, `credit`, `reverse` responses.
2. On timeout, PSP performs status query before deciding next state.
3. Retry count: maximum `2` retries per operation.
4. Backoff: deterministic (`1s`, `2s`) based on attempt number.
5. If retries are exhausted:
   - debit unresolved -> `FAILED`
   - credit unresolved after debit success -> `COMPENSATION_IN_PROGRESS`
   - reverse unresolved -> keep retry policy active until terminal resolution path is achieved by design policy.

## Idempotency Rules

1. PSP create request: unique `idempotencyKey` per client intent.
2. Bank operation idempotency: unique `(transactionId, operationType)` in `processed_operations`.
3. Duplicate request must return stored result, never re-apply side effect.

## Exactly-Once Simulation Semantics

Exactly-once is simulated through:

1. Monotonic state transitions at PSP.
2. Idempotent operation execution at bank.
3. Persistent queue with row locking at PSP.
4. Timeout resolution by status query, not guesswork.
