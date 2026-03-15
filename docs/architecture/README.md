# Architecture Artifacts

This folder contains editable source for your core design figures.

## Files

- `distributed-workflow.mmd`: Full layered architecture/workflow (Mermaid, importable in diagrams.net).
- `layered-architecture.drawio`: Styled diagrams.net figure (directly editable; icon-ready).
- `psp-erd.puml`: Detailed PSP schema (transaction lifecycle, queue, failures, experiments).
- `bank-erd.puml`: Detailed bank schema (accounts, immutable ledger, idempotent processing).
- `implementation-roadmap.md`: Practical step-by-step execution order from design to coding.
- `service-api-contracts.md`: Formal API, timeout, retry, and idempotency contract.
- `transaction-state-machine.puml`: Deterministic transaction lifecycle and terminal guarantees.
- `distributed-saga-sequence.puml`: PSP-centric distributed saga interaction flow.
- `psp-saga-activity.puml`: Deterministic orchestration activity with timeout handling.
- `psp-execution-engine-component.puml`: PSP internal persistent-queue execution architecture.
- `psp-worker-loop-activity.puml`: Worker processing loop over persistent queue.
- `psp-worker-locking-sequence.puml`: Concurrency and `SKIP LOCKED` behavior.
- `failure-injection-architecture.puml`: Controlled deterministic failure injection model.
- `observability-architecture.puml`: Audit, metrics, and determinism verification layout.

## diagrams.net import

1. Open diagrams.net.
2. Use `Arrange -> Insert -> Advanced -> Mermaid`.
3. Paste content from `distributed-workflow.mmd`.
4. Replace generic boxes with icons (mobile, service, db, dashboard) and keep labels unchanged.

## Direct diagrams.net workflow

1. Open `layered-architecture.drawio` in diagrams.net.
2. Use `More Shapes...` to enable icon libraries (AWS/Azure/Networking if needed).
3. Replace emoji blocks with your preferred icon shapes while preserving text labels and connectors.
4. Export as SVG/PNG for report and PPT.

## PlantUML render

Render `.puml` files with any PlantUML plugin or CLI to generate ERD figures for report/slides.
