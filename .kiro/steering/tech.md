# Technology Stack & Build System

## Runtime & Language

- **Java 25** (`maven.compiler.source/target` in module `pom.xml`; Lambda `Runtime: java25`) - all Lambda functions
- **Maven** - build and dependency management; root `pom.xml` aggregates the service modules
- **AWS SAM** - infrastructure as code, build, and deployment

## AWS Services

- **AWS Lambda** - serverless compute
- **Amazon DynamoDB** - NoSQL persistence with streams
- **Amazon EventBridge** - event bus, schema registry, and cross-service messaging
- **AWS Step Functions** - approval workflow orchestration
- **Amazon API Gateway** - REST API endpoints
- **Amazon SQS** - ingest queues and dead-letter queues
- **AWS X-Ray** - distributed tracing
- **Amazon CloudWatch** - logging, metrics, and monitoring

## Key Libraries & Frameworks

- **AWS Lambda Powertools for Java** (`software.amazon.lambda`: `powertools-logging`, `powertools-metrics`, `powertools-tracing`) - structured logging, metrics, and tracing
- **AWS SDK for Java v2** (`dynamodb`, `dynamodb-enhanced`, `eventbridge`) plus `aws-lambda-java-core`/`events`
- **JUnit 5 (Jupiter)** with **Mockito** and `aws-lambda-java-tests` - unit testing
- **maven-shade-plugin** builds deployable fat JARs; **maven-surefire-plugin** runs tests

## Build & Development Commands

### Make Targets (canonical interface)

Run from a service directory (e.g., `unicorn_contracts/`). Stages: `local` (default), `dev`, `prod`; default region `ap-southeast-2`.

```bash
make build STAGE=local      # mvn clean compile, then sam build
make deploy STAGE=local     # deploy-domain + deploy-service + deploy-schema
make deploy-domain          # Event bus, schema registry (infrastructure/domain.yaml)
make deploy-schema          # Event schema stack(s)
make deploy-service         # Lambda, API Gateway, DynamoDB (service template)
make test                   # Run tests (mvn test)
make lint                   # cfn-lint all infrastructure templates
make clean                  # Clean build artifacts
make delete                 # Delete stacks in reverse dependency order
```

`unicorn_shared/` has its own targets for namespaces and shared images stacks. Deploy shared namespaces before any service.

### Runtime Commands

Inside a service directory, the make targets invoke Maven commands you can also run directly:

```bash
mvn clean compile -f <Name>Service/pom.xml   # Compile a module
mvn test -f <Name>Service/pom.xml            # Run JUnit tests
mvn clean package -f <Name>Service/pom.xml   # Build shaded deployment JAR
```

### SAM Commands

```bash
sam build --cached --parallel                  # Build (config in samconfig.toml)
sam deploy --no-confirm-changeset              # Deploy current service
sam validate --lint                            # Validate templates
sam sync --watch                               # Rapid dev iteration
sam local start-api --warm-containers EAGER    # Local API
sam local start-lambda --warm-containers EAGER # Local Lambda endpoint
```

`samconfig.toml` in each `infrastructure/<service>-service/` directory sets stack name, cached/parallel builds, `disable_rollback` for dev iteration, and `Stage` parameter overrides.

## Environment Variables

Standard Lambda environment variables set in the SAM templates:

- `DYNAMODB_TABLE` - DynamoDB table name
- `SERVICE_NAMESPACE` - service identifier for event sources (from SSM namespace parameters)
- `POWERTOOLS_SERVICE_NAME`, `POWERTOOLS_METRICS_NAMESPACE` - Powertools identifiers
- `POWERTOOLS_LOG_LEVEL`, `POWERTOOLS_LOGGER_CASE`, `POWERTOOLS_LOGGER_LOG_EVENT`, `POWERTOOLS_LOGGER_SAMPLE_RATE`, `POWERTOOLS_TRACE_DISABLED` - observability tuning (stage-mapped)
- `LOG_LEVEL` - application log level
