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
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContractEventHandler implements RequestHandler<SQSEvent, Void> {

    private static final String DDB_TABLE = System.getenv("DYNAMODB_TABLE");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LogManager.getLogger(ContractEventHandler.class);

    private DynamoDbClient dynamodbClient = DynamoDbClient.builder().build();
    private final MetricsLogger metricsLogger = MetricsUtils.metricsLogger();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSMessage msg : event.getRecords()) {
            LOGGER.debug("Processing message: {}", msg.getMessageId());
            
            String httpMethod = msg.getMessageAttributes().get("HttpMethod").getStringValue();
            try {
                if ("POST".equalsIgnoreCase(httpMethod)) {
                    createContract(msg.getBody());
                    LOGGER.info("Contract created successfully");
                } else if ("PUT".equalsIgnoreCase(httpMethod)) {
                    updateContract(msg.getBody());
                    LOGGER.info("Contract updated successfully");
                }
            } catch (JsonProcessingException e) {
                LOGGER.error("JSON processing error for message {}: {}", msg.getMessageId(), e.getMessage());
                throw new RuntimeException("Failed to process contract", e);
            }
        }
        return null;
    }

    @Tracing
    private void createContract(String strContract) throws JsonProcessingException {
        String contractId = UUID.randomUUID().toString();
        long createDate = Instant.now().toEpochMilli();
        Contract contract = OBJECT_MAPPER.readValue(strContract, Contract.class);

        Map<String, AttributeValue> expressionValues = Map.of(
            ":cancelled", AttributeValue.builder().s(ContractStatusEnum.CANCELLED.name()).build(),
            ":closed", AttributeValue.builder().s(ContractStatusEnum.CLOSED.name()).build(),
            ":expired", AttributeValue.builder().s(ContractStatusEnum.EXPIRED.name()).build()
        );

        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("property_id", AttributeValue.builder().s(contract.getPropertyId()).build());
        itemValues.put("seller_name", AttributeValue.builder().s(contract.getSellerName()).build());
        itemValues.put("contract_created", AttributeValue.builder().n(String.valueOf(createDate)).build());
        itemValues.put("contract_last_modified_on", AttributeValue.builder().n(String.valueOf(createDate)).build());
        itemValues.put("contract_id", AttributeValue.builder().s(contractId).build());
        itemValues.put("contract_status", AttributeValue.builder().s(ContractStatusEnum.DRAFT.name()).build());

        Map<String, AttributeValue> address = Map.of(
            "country", AttributeValue.builder().s(contract.getAddress().getCountry()).build(),
            "city", AttributeValue.builder().s(contract.getAddress().getCity()).build(),
            "street", AttributeValue.builder().s(contract.getAddress().getStreet()).build(),
            "number", AttributeValue.builder().n(String.valueOf(contract.getAddress().getNumber())).build()
        );

        itemValues.put("address", AttributeValue.builder().m(address).build());
        
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(DDB_TABLE)
                .item(itemValues)
                .conditionExpression("attribute_not_exists(property_id) OR contract_status IN (:cancelled , :closed, :expired)")
                .expressionAttributeValues(expressionValues)
                .build();
        
        try {
            dynamodbClient.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException e) {
            LOGGER.error("Unable to create contract for Property '{}'. Active contract already exists", 
                contract.getPropertyId());
            throw new RuntimeException("Contract already exists for property", e);
        }
    }

    @Tracing
    private void updateContract(String strContract) throws JsonProcessingException {
        Contract contract = OBJECT_MAPPER.readValue(strContract, Contract.class);
        LOGGER.info("Updating contract for Property ID: {}", contract.getPropertyId());
        
        Map<String, AttributeValue> itemKey = Map.of(
            "property_id", AttributeValue.builder().s(contract.getPropertyId()).build()
        );

        Map<String, AttributeValue> expressionAttributeValues = Map.of(
            ":draft", AttributeValue.builder().s(ContractStatusEnum.DRAFT.name()).build(),
            ":approved", AttributeValue.builder().s(ContractStatusEnum.APPROVED.name()).build(),
            ":modifiedDate", AttributeValue.builder().n(String.valueOf(Instant.now().toEpochMilli())).build()
        );

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(DDB_TABLE)
                .key(itemKey)
                .updateExpression("set contract_status=:approved, modified_date=:modifiedDate")
                .expressionAttributeValues(expressionAttributeValues)
                .conditionExpression("attribute_exists(property_id) AND contract_status IN (:draft)")
                .build();
        
        try {
            dynamodbClient.updateItem(request);
        } catch (ConditionalCheckFailedException e) {
            LOGGER.error("Unable to update contract for Property '{}'. Status is not DRAFT", contract.getPropertyId());
            throw new RuntimeException("Contract not in valid state for update", e);
        } catch (ResourceNotFoundException e) {
            LOGGER.error("Contract not found for Property '{}'", contract.getPropertyId());
            throw new RuntimeException("Contract not found", e);
        }
    }

    public void setDynamodbClient(DynamoDbClient dynamodbClient) {
        this.dynamodbClient = dynamodbClient;
    }
}
