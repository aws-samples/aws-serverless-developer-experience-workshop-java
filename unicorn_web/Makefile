# Makefile for Unicorn Web Service Infrastructure

# Variables
STAGE ?= local
REGION ?= ap-southeast-2
STACK_PREFIX = uni-prop-$(STAGE)

.PHONY: help build deploy deploy-domain deploy-service deploy-schema deploy-subscriptions test lint clean delete

help: ## Show available commands
	@echo "Unicorn Web Service Infrastructure"
	@echo ""
	@echo "Usage: make [target] [STAGE=local|dev|prod] [REGION=ap-southeast-2]"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-15s %s\n", $1, $2}'

build: ## Build the application
	@echo "Building Java application..."
	@mvn clean compile -f SearchService/pom.xml
	@mvn clean compile -f PublicationManagerService/pom.xml
	@sam build --template-file infrastructure/web-service.yaml

deploy-domain: ## Deploy domain resources (EventBridge, Schema Registry)
	@echo "Deploying domain resources..."
	@sam deploy \
		--template-file infrastructure/domain.yaml \
		--stack-name $(STACK_PREFIX)-web-domain \
		--parameter-overrides Stage=$(STAGE) \
		--capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND \
		--region $(REGION) \
		--resolve-s3 \
		--no-confirm-changeset \
		--no-fail-on-empty-changeset

deploy-service: build ## Deploy service resources (Lambda, API Gateway, DynamoDB)
	@echo "Deploying service resources..."
	@sam deploy \
		--template-file infrastructure/web-service.yaml \
		--stack-name $(STACK_PREFIX)-web-service \
		--parameter-overrides Stage=$(STAGE) \
		--capabilities CAPABILITY_IAM CAPABILITY_AUTO_EXPAND \
		--region $(REGION) \
		--resolve-s3 \
		--no-confirm-changeset \
		--no-fail-on-empty-changeset

deploy-schema: ## Deploy event schema
	@echo "Deploying event schema..."
	@sam deploy \
		--template-file infrastructure/schema-registry/PublicationApprovalRequested-schema.yaml \
		--stack-name $(STACK_PREFIX)-web-schema-PublicationApprovalRequested \
		--parameter-overrides Stage=$(STAGE) \
		--capabilities CAPABILITY_IAM \
		--region $(REGION) \
		--resolve-s3 \
		--no-confirm-changeset \
		--no-fail-on-empty-changeset

deploy-subscriptions: ## Deploy event subscriptions
	@echo "Deploying approvals subscriptions..."
	@sam deploy \
		--template-file infrastructure/subscriptions/unicorn-approvals-subscriptions.yaml \
		--stack-name $(STACK_PREFIX)-web-approvals-subscriptions \
		--parameter-overrides Stage=$(STAGE) \
		--capabilities CAPABILITY_IAM \
		--region $(REGION) \
		--resolve-s3 \
		--no-confirm-changeset \
		--no-fail-on-empty-changeset

deploy: deploy-domain deploy-service deploy-schema deploy-subscriptions ## Deploy all infrastructure and application

test: ## Run tests
	@echo "Running tests..."
	@mvn test -f SearchService/pom.xml
	@mvn test -f PublicationManagerService/pom.xml

lint: ## Lint CloudFormation templates
	@echo "Linting CloudFormation templates..."
	@cfn-lint infrastructure/web-service.yaml -a cfn_lint_serverless.rules
	@cfn-lint infrastructure/domain.yaml
	@cfn-lint infrastructure/schema-registry/PublicationApprovalRequested-schema.yaml
	@cfn-lint infrastructure/subscriptions/unicorn-approvals-subscriptions.yaml

clean: ## Clean build artifacts
	@echo "Cleaning build artifacts..."
	@rm -rf .aws-sam/
	@mvn clean -f SearchService/pom.xml
	@mvn clean -f PublicationManagerService/pom.xml

delete: ## Delete all stacks
	@echo "Deleting stacks in reverse order..."
	@aws cloudformation delete-stack --stack-name $(STACK_PREFIX)-web-approvals-subscriptions --region $(REGION) || true
	@echo "Waiting for subscription stacks to be deleted..."
	@aws cloudformation wait stack-delete-complete --stack-name $(STACK_PREFIX)-web-approvals-subscriptions --region $(REGION) || true
	@aws cloudformation delete-stack --stack-name $(STACK_PREFIX)-web-schema-PublicationApprovalRequested --region $(REGION) || true
	@aws cloudformation delete-stack --stack-name $(STACK_PREFIX)-web-service --region $(REGION) || true
	@echo "Waiting for schema and service stacks to be deleted..."
	@aws cloudformation wait stack-delete-complete --stack-name $(STACK_PREFIX)-web-schema-PublicationApprovalRequested --region $(REGION) || true
	@aws cloudformation wait stack-delete-complete --stack-name $(STACK_PREFIX)-web-service --region $(REGION) || true
	@aws cloudformation delete-stack --stack-name $(STACK_PREFIX)-web-domain --region $(REGION) || true
	@echo "Waiting for domain stack to be deleted..."
	@aws cloudformation wait stack-delete-complete --stack-name $(STACK_PREFIX)-web-domain --region $(REGION) || true
	@echo "All stacks deleted"