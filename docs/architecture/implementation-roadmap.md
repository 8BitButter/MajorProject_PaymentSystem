# Implementation Roadmap (Distributed Deterministic PSP Model)

## Current Implementation Status (March 8, 2026)

1. Completed:
   - Correlation ID propagation and structured transaction logging.
   - Deterministic timeout + retry policy in PSP execution flow.
   - Persistent execution queue (`execution_queue`) with worker claim/release/retry.
   - Bank idempotency store (`processed_operations`) for debit/credit/reversal.
   - Bank operation status query endpoint for timeout resolution.
   - Switch and Bank operation APIs (`debit`, `credit`, `reverse`) with shared request/response DTOs.
   - Admin growth dashboard endpoint for KPI snapshot + recent transaction monitoring.
2. In progress:
   - Tightening failure-injection scenarios around switch/bank route-level APIs.
   - Expanding integration tests for switch-facing endpoints and queue recovery edge cases.

## Starting Focus (Do This First)

1. Freeze the global transaction state machine before adding new APIs or services.
2. Freeze legal transitions, timeout resolution, and compensation semantics.
3. Freeze idempotency rules at PSP and Bank boundaries.

Reference:
- `transaction-state-machine.puml`
- `psp-saga-activity.puml`

## Phase 1: Distributed Architecture Design

1. Finalize service boundaries:
   - PSP: saga orchestration, global state ownership, idempotency, queue workers.
   - Switch: routing + delay/error injection, no core business ownership.
   - Bank: debit/credit/reverse with immutable ledger + idempotency.
2. Freeze inter-service API contracts and request/response payloads.
3. Freeze timeout behavior and retry policy per operation.
4. Standardize correlation data on every call/log: `transactionId`, `correlationId`, `timestamp`.

Reference:
- `service-api-contracts.md`
- `distributed-saga-sequence.puml`

## Phase 2: Deterministic Orchestration Design

1. Lock saga steps and legal transitions.
2. Lock compensation behavior for post-debit failures.
3. Lock deterministic timeout resolution rules (query status, then advance).
4. Lock retry behavior with monotonic state progression only.
5. Lock exactly-once simulation rules using idempotency + processed operation checks.

Reference:
- `transaction-state-machine.puml`
- `psp-saga-activity.puml`

## Phase 3: Data Ownership Model

1. PSP DB owns:
   - `transactions`
   - `transaction_state_events` / logs
   - `execution_queue`
   - `idempotency_records` (or equivalent key mapping)
2. Bank DB owns:
   - `accounts`
   - `ledger_entries`
   - `processed_operations` (idempotency)
3. Switch remains stateless or uses minimal operational logging only.
4. Keep ownership strict for viva clarity and correctness reasoning.

Reference:
- `psp-erd.puml`
- `bank-erd.puml`

## Phase 4: Failure Model (Controlled + Reproducible)

1. Inject failures at deterministic points:
   - PSP before debit
   - Switch during routing
   - Bank during debit
   - Bank during credit
   - Network timeout
   - Duplicate request replay
2. Define deterministic expected outcome for each injection point.
3. Log all injected failures with policy metadata and attempt number.

Reference:
- `failure-injection-architecture.puml`

## Phase 5: Offline Mode Design

1. Offline requests go to PSP queue, not directly to banks.
2. PSP validates and executes full saga when processing window opens.
3. Define reconciliation policy:
   - duplicate prevention
   - stale request handling
   - terminal status guarantee

## Phase 6: Priority Scheduling (Distributed)

1. Apply prioritization at PSP before orchestration steps start.
2. Use persistent priority queue + worker pool.
3. Control execution rate via scheduler/worker tuning.
4. Preserve determinism and idempotency under concurrency.

Reference:
- `psp-execution-engine-component.puml`
- `psp-worker-loop-activity.puml`
- `psp-worker-locking-sequence.puml`

## Phase 7: Startup Productization Layer (New)

1. Merchant features:
   - Merchant API keys + webhook subscriptions (`payment.succeeded`, `payment.failed`, `payment.reversed`).
   - Merchant dashboard: success rate, payout lag, dispute trends, hourly volume.
   - Hosted checkout and payment links with expiry + branding.
2. User trust features:
   - Recipient verification score before pay.
   - Risk hints (velocity, amount anomaly, destination freshness).
   - In-app dispute and refund workflow with status timeline.
3. Growth features:
   - Collect requests and dynamic QR flows (in addition to push).
   - Offer/cashback rule engine with controlled experiments.
   - Referral and cohort analytics.
4. Enterprise features:
   - Multi-tenant org model (tenant-scoped data + limits + configs).
   - Role-based admin (ops, support, finance, auditor).
   - SLA dashboard and incident timeline for compliance/audit.

## Phase 8: Revenue + Distribution Engine (New)

1. Pricing system:
   - Configurable MDR/fee slabs by merchant segment.
   - Settlement reports and downloadable invoice artifacts.
2. Settlement and payouts:
   - Daily settlement batch simulation with reconciliation diffs.
   - Merchant wallet + scheduled payout cycle simulation.
3. Integrations:
   - ERP/accounting export adapters.
   - Public developer portal with sandbox credentials and docs.
4. GTM readiness:
   - Demo tenant bootstrap script for sales demos.
   - Product analytics funnel: signup -> API key -> first successful payment.

## Top Things To Do Next (Priority Ordered)

1. Build `merchant` domain + API key auth boundary; keep simulator internals private.
2. Add merchant webhooks with retry + signature verification.
3. Add collect/QR APIs and extend transaction FSM with pull-request states.
4. Add tenant-aware data model (`tenant_id` on all operational tables).
5. Build real-time ops dashboard widgets (queue lag, timeout ratio, failure hotspot).
6. Implement dispute/refund flow with auditable decision events.
7. Add billing/fee computation module and monthly statement generation.
8. Add API docs site (OpenAPI + runnable examples) for external onboarding.

## Cross-Cutting Requirements (Must Hold Across All Phases)

1. PSP idempotency on create/retry.
2. Bank idempotency on debit/credit/reverse.
3. Global correlation ID strategy for all service logs.
4. Deterministic timeout + retry semantics.
5. Exactly-once simulation behavior.

## Suggested Development Timeline

1. Month 1: PSP + one Bank with success flow.
2. Month 2: Add Switch layer and routing contracts.
3. Month 3: Add deterministic failure injection.
4. Month 4: Add idempotency hardening + offline queue path.
5. Month 5: Load testing + priority scheduler tuning + evaluation.
6. Month 6: Merchant onboarding + webhook + dashboard package.
7. Month 7: Collect/QR + dispute/refund lifecycle.
8. Month 8: Tenantization + pricing + settlement exports.
