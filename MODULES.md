# DIPS Module Implementation Map

This file maps the approved module plan (M0-M14) to current implementation artifacts.

## M0 Project Foundation
- `backend/pom.xml`
- `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/*`
- `.github/workflows/ci.yml`
- `docker-compose.yml`

## M1 Domain & FSM Core
- `backend/src/main/java/com/dips/simulator/domain/enums/TransactionState.java`
- `backend/src/main/java/com/dips/simulator/service/TransactionFsmService.java`
- `backend/src/main/java/com/dips/simulator/service/TransactionStateService.java`

## M2 Data Layer
- `backend/src/main/resources/db/migration/V1__init.sql`
- `backend/src/main/java/com/dips/simulator/domain/*`
- `backend/src/main/java/com/dips/simulator/repository/*`

## M3 Payment Initiation API
- `backend/src/main/java/com/dips/simulator/controller/PaymentController.java`
- `POST /api/payments/push`

## M4 PSP Orchestrator
- `backend/src/main/java/com/dips/simulator/service/PaymentExecutionService.java`
- `backend/src/main/java/com/dips/simulator/service/PaymentService.java`

## M5 Payment Switch Router
- `backend/src/main/java/com/dips/simulator/service/PaymentSwitchService.java`

## M6 Virtual Banks & Ledger
- `backend/src/main/java/com/dips/simulator/service/VirtualBankService.java`
- `backend/src/main/java/com/dips/simulator/domain/LedgerEntryEntity.java`

## M7 Reversal & Compensation
- `backend/src/main/java/com/dips/simulator/service/PaymentExecutionService.java` (credit-fail -> reversal path)

## M8 Realtime Event Stream
- `backend/src/main/java/com/dips/simulator/config/WebSocketConfig.java`
- `backend/src/main/java/com/dips/simulator/websocket/*`
- `backend/src/main/java/com/dips/simulator/service/StreamPublisher.java`

## M9 Web UX + Sequence Animation
- `backend/src/main/resources/static/index.html`
- `backend/src/main/resources/static/payee.html`
- `backend/src/main/resources/static/app.js`
- `backend/src/main/resources/static/payee.js`

## M10 Offline SMS Encrypted Flow
- `backend/src/main/java/com/dips/simulator/controller/OfflineSmsController.java`
- `backend/src/main/java/com/dips/simulator/service/OfflineSmsService.java`
- `backend/src/main/java/com/dips/simulator/service/crypto/SmsCryptoService.java`

## M11 Priority Scheduler
- `backend/src/main/java/com/dips/simulator/service/scheduler/PrioritySchedulerService.java`
- `backend/src/main/java/com/dips/simulator/config/SchedulerProperties.java`
- `backend/src/main/java/com/dips/simulator/service/LoadProfileService.java`

## M12 Failure Injection Console
- `backend/src/main/java/com/dips/simulator/controller/AdminController.java`
- `backend/src/main/java/com/dips/simulator/service/FailureInjectionService.java`
- `backend/src/main/resources/static/admin.html`
- `backend/src/main/resources/static/admin.js`

## M13 Observability & Audit
- `backend/src/main/java/com/dips/simulator/domain/TransactionEventEntity.java`
- `GET /api/payments/{txId}/events`
- actuator endpoints (`/actuator/health`, `/actuator/metrics`)

## M14 QA, Performance, Final Demo
- `backend/src/test/java/com/dips/simulator/PaymentFlowIntegrationTest.java`
- `README.md` runbook and demo URLs

