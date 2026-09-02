# Project Structure & Conventions

## Repository Layout

Multi-service monorepo with three services plus shared infrastructure:

```text
├── unicorn_contracts/     # Contracts Service - property contracts
├── unicorn_approvals/     # Approvals Service - approval workflow
├── unicorn_web/           # Web Service - property listings and search
├── unicorn_shared/        # Global namespaces and shared images stacks
├── docs/                  # Documentation and architecture diagrams
└── pom.xml                # Root Maven aggregator (modules only, no dependencies)
```

## Service Structure Pattern

Each service follows this structure. Maintain it when adding or modifying services:

```text
unicorn_<service>/
├── Makefile                  # Canonical build/deploy/test interface
├── <Name>Service/            # Maven module (PascalCase)
│   ├── pom.xml               # Module dependencies and build config
│   └── src/
│       ├── main/java/        # Handlers and domain code
│       ├── main/resources/   # Logging configuration
│       └── test/             # Unit tests + test/events/ payloads
├── events/                   # Sample event payloads for manual testing
└── infrastructure/           # All IaC for the service (see below)
```

`unicorn_web` contains multiple Maven modules (`PublicationManagerService`, `SearchService`, plus shared `Common` and `Data`). Step Functions ASL definitions live next to the service template (e.g., `unicorn_approvals/infrastructure/approvals-service/property_approval.asl.yaml`).

## Infrastructure Layout

Every service owns its infrastructure under `infrastructure/`:

```text
infrastructure/
├── domain.yaml                     # Event bus, bus policies, schema registry,
│                                   #   catch-all rule, SSM exports
├── <service>-service/
│   ├── template.yaml               # Lambda, API Gateway, DynamoDB, queues
│   ├── samconfig.toml              # SAM build/deploy configuration
│   └── api.yaml                    # OpenAPI specification (REST services)
├── schema-registry/
│   └── <EventName>-schema.yaml     # One stack per published event schema
└── subscriptions/
    └── <producer>-subscriptions.yaml  # Rules on producer buses feeding this service
```

Deploy order: shared namespaces first, then per service `domain -> service -> schema` (see the `deploy` target); subscriptions reference both participating domains. `make deploy` and `make delete` encode the correct (reverse) order.

## Code Conventions

- Maven modules are PascalCase (`ContractsService`, `ApprovalsService`, `PublicationManagerService`, `SearchService`)
- Root packages match the service domain: `contracts`, `approvals`, plus `schema.unicorn_<service>.*` for event models and `<service>.dao` for data access
- Handler classes end with `Function` (`ContractEventHandlerFunction`); place them in the root service package
- The root `pom.xml` is an aggregator only — service dependencies belong in module `pom.xml` files
- Shared Web utilities live in the `unicorn_web/Common` module

## Resource Naming & Events

- Stack names: `uni-prop-{stage}-{service}` (e.g., `uni-prop-local-contracts`); schema stacks append `-schema-<EventName>`
- Event buses: `unicorn-{service}-eventbus-${Stage}` (e.g., `unicorn-contracts-eventbus-local`), exported via SSM
- Event sources use the service namespace value (e.g., `unicorn-contracts`), resolved from the SSM namespace parameters — never hardcode it
- Canonical event detail types: `ContractStatusChanged` (Contracts), `PublicationApprovalRequested` (Web), `PublicationEvaluationCompleted` (Approvals)
- SSM parameter contracts:
  - Global namespaces: `/uni-prop/Unicorn{Service}Namespace`
  - Stage-scoped: `/uni-prop/${Stage}/{Service}EventBus`, `...EventBusArn`, `...SchemaRegistryName`
- Each domain template includes a catch-all rule logging all bus events to CloudWatch for debugging

## Testing Conventions

```text
<Name>Service/src/test/
├── java/<package>/        # JUnit 5 tests mirroring source structure (Mockito mocks)
└── events/                # Sample event payloads (e.g., create_contract_valid_1.json)
```

- Test event naming: `[action]_[entity]_[condition]_[n].json` (e.g., `create_contract_valid_1.json`)
- Service-root `events/` holds payloads for manual invocation and debugging
- Run tests with `make test` (wraps `mvn test`)

## Development Workflow

When adding a feature:

1. Implement the handler in the service's Maven module following existing patterns
2. Add or update SAM resources in `infrastructure/<service>-service/template.yaml` using the naming conventions above
3. Add unit test payloads under `src/test/events/` and tests beside existing ones
4. If the change publishes or consumes a new event: update `infrastructure/schema-registry/` and the consumer's `infrastructure/subscriptions/`
5. Run `make lint` and `make test` before deploying with `make deploy STAGE=local`

When modifying services, preserve the existing structure — do not reorganize directories without a documented reason.
