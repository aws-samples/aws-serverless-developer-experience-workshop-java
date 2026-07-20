package contracts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.utils.Contract;
import contracts.utils.ContractStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.FlushMetrics;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsFactory;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ContractEventHandlerFunction implements RequestHandler<SQSEvent, Void> {

    private static final String DDB_TABLE = System.getenv("DYNAMODB_TABLE");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LogManager.getLogger(ContractEventHandlerFunction.class);
    private static final String HTTP_METHOD_ATTR = "HttpMethod";

    private final DynamoDbClient dynamodbClient;

    public ContractEventHandlerFunction() {
        this(DynamoDbClient.builder().build());
    }

    public ContractEventHandlerFunction(DynamoDbClient dynamodbClient) {
        this.dynamodbClient = dynamodbClient;
    }

    @Override
    @Tracing
    @FlushMetrics(captureColdStart = true)
    @Logging(logEvent = true)
    public Void handleRequest(SQSEvent event, Context context) {
        if (event == null || event.getRecords() == null) {
            LOGGER.warn("Received null or empty SQS event");
            return null;
        }

        for (SQSMessage msg : event.getRecords()) {
            processMessage(msg);
        }
        return null;
    }

    private void processMessage(SQSMessage msg) {
        LOGGER.debug("Processing message: {}", msg.getMessageId());

        try {
            String httpMethod = extractHttpMethod(msg);
            String body = msg.getBody();

            if (body == null || body.trim().isEmpty()) {
                LOGGER.warn("Empty message body for message: {}", msg.getMessageId());
                return;
            }

            switch (httpMethod.toUpperCase()) {
                case "POST":
                    createContract(body);
                    LOGGER.info("Contract created successfully for message: {}", msg.getMessageId());
                    break;
                case "PUT":
                    updateContract(body);
                    LOGGER.info("Contract updated successfully for message: {}", msg.getMessageId());
                    break;
                default:
                    LOGGER.warn("Unsupported HTTP method: {} for message: {}", httpMethod, msg.getMessageId());
            }
        } catch (Exception e) {
            LOGGER.error("Error processing message {}: {}", msg.getMessageId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process contract message", e);
        }
    }

    private String extractHttpMethod(SQSMessage msg) {
        return Optional.ofNullable(msg.getMessageAttributes())
                .map(attrs -> attrs.get(HTTP_METHOD_ATTR))
                .map(attr -> attr.getStringValue())
                .orElseThrow(() -> new IllegalArgumentException("Missing HttpMethod attribute"));
    }

    @Tracing
    private void createContract(String contractJson) throws JsonProcessingException {
        Contract contract = OBJECT_MAPPER.readValue(contractJson, Contract.class);
        validateContract(contract);

        String contractId = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        Map<String, AttributeValue> item = Map.of(
            "property_id", AttributeValue.builder().s(contract.getPropertyId()).build(),
            "seller_name", AttributeValue.builder().s(contract.getSellerName()).build(),
            "contract_created", AttributeValue.builder().s(timestamp).build(),
            "contract_last_modified_on", AttributeValue.builder().s(timestamp).build(),
            "contract_id", AttributeValue.builder().s(contractId).build(),
            "contract_status", AttributeValue.builder().s(ContractStatusEnum.DRAFT.name()).build(),
            "address", AttributeValue.builder().m(buildAddressMap(contract.getAddress())).build()
        );

        Map<String, AttributeValue> expressionValues = Map.of(
            ":cancelled", AttributeValue.builder().s(ContractStatusEnum.CANCELLED.name()).build(),
            ":closed", AttributeValue.builder().s(ContractStatusEnum.CLOSED.name()).build(),
            ":expired", AttributeValue.builder().s(ContractStatusEnum.EXPIRED.name()).build()
        );

        PutItemRequest request = PutItemRequest.builder()
                .tableName(DDB_TABLE)
                .item(item)
                .conditionExpression("attribute_not_exists(property_id) OR contract_status IN (:cancelled, :closed, :expired)")
                .expressionAttributeValues(expressionValues)
                .build();

        try {
            dynamodbClient.putItem(request);
            MetricsFactory.getMetricsInstance().addMetric("ContractCreated", 1, MetricUnit.COUNT);
        } catch (ConditionalCheckFailedException e) {
            LOGGER.error("Active contract already exists for property: {}", contract.getPropertyId());
            throw new IllegalStateException("Contract already exists for property: " + contract.getPropertyId(), e);
        }
    }

    @Tracing
    private void updateContract(String contractJson) throws JsonProcessingException {
        Contract contract = OBJECT_MAPPER.readValue(contractJson, Contract.class);
        validateContractForUpdate(contract);

        LOGGER.info("Updating contract for Property ID: {}", contract.getPropertyId());

        Map<String, AttributeValue> key = Map.of(
            "property_id", AttributeValue.builder().s(contract.getPropertyId()).build()
        );

        Map<String, AttributeValue> expressionValues = Map.of(
            ":draft", AttributeValue.builder().s(ContractStatusEnum.DRAFT.name()).build(),
            ":approved", AttributeValue.builder().s(ContractStatusEnum.APPROVED.name()).build(),
            ":modifiedDate", AttributeValue.builder().s(Instant.now().toString()).build()
        );

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(DDB_TABLE)
                .key(key)
                .updateExpression("SET contract_status = :approved, contract_last_modified_on = :modifiedDate")
                .expressionAttributeValues(expressionValues)
                .conditionExpression("attribute_exists(property_id) AND contract_status = :draft")
                .build();

        try {
            dynamodbClient.updateItem(request);
            MetricsFactory.getMetricsInstance().addMetric("ContractUpdated", 1, MetricUnit.COUNT);
        } catch (ConditionalCheckFailedException e) {
            LOGGER.error("Contract not in DRAFT status for property: {}", contract.getPropertyId());
            throw new IllegalStateException("Contract not in valid state for update: " + contract.getPropertyId(), e);
        } catch (ResourceNotFoundException e) {
            LOGGER.error("Contract not found for property: {}", contract.getPropertyId());
            throw new IllegalArgumentException("Contract not found: " + contract.getPropertyId(), e);
        }
    }

    private void validateContract(Contract contract) {
        if (contract == null) {
            throw new IllegalArgumentException("Contract cannot be null");
        }
        if (contract.getPropertyId() == null || contract.getPropertyId().trim().isEmpty()) {
            throw new IllegalArgumentException("Property ID is required");
        }
        if (contract.getSellerName() == null || contract.getSellerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Seller name is required");
        }
        if (contract.getAddress() == null) {
            throw new IllegalArgumentException("Address is required");
        }
    }

    private void validateContractForUpdate(Contract contract) {
        if (contract == null) {
            throw new IllegalArgumentException("Contract cannot be null");
        }
        if (contract.getPropertyId() == null || contract.getPropertyId().trim().isEmpty()) {
            throw new IllegalArgumentException("Property ID is required for update");
        }
    }

    private Map<String, AttributeValue> buildAddressMap(contracts.utils.Address address) {
        return Map.of(
            "country", AttributeValue.builder().s(address.getCountry()).build(),
            "city", AttributeValue.builder().s(address.getCity()).build(),
            "street", AttributeValue.builder().s(address.getStreet()).build(),
            "number", AttributeValue.builder().n(String.valueOf(address.getNumber())).build()
        );
    }
}
