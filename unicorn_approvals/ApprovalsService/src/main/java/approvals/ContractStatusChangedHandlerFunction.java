package approvals;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import schema.unicorn_contracts.contractstatuschanged.ContractStatusChanged;
import schema.unicorn_contracts.contractstatuschanged.Event;
import schema.unicorn_contracts.contractstatuschanged.marshaller.Marshaller;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Lambda handler to update the contract status change
 */
public class ContractStatusChangedHandlerFunction {

    private static final Logger logger = LogManager.getLogger();
    private static final String TABLE_NAME = System.getenv("CONTRACT_STATUS_TABLE");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private DynamoDbClient dynamodbClient = DynamoDbClient.builder().build();

    /**
     * Handles contract status change events from EventBridge
     * 
     * @param inputStream  the input stream containing the event
     * @param outputStream the output stream for the response
     * @param context      the Lambda context
     * @throws IOException if there's an error processing the event
     */
    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true)
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        logger.info("TABLE_NAME environment variable: {}", TABLE_NAME);
        
        Event event = Marshaller.unmarshal(inputStream, Event.class);
        ContractStatusChanged contractStatusChanged = event.getDetail();
        logger.info("Received event: {}", contractStatusChanged);
        
        try {
            saveContractStatus(
                contractStatusChanged.getPropertyId(), 
                contractStatusChanged.getContractStatus(),
                contractStatusChanged.getContractId(),
                contractStatusChanged.getContractLastModifiedOn()
            );
            logger.info("Successfully updated contract status for property: {}", contractStatusChanged.getPropertyId());
        } catch (Exception e) {
            logger.error("Failed to update contract status for property: {}", contractStatusChanged.getPropertyId(), e);
            throw e;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(objectMapper.writeValueAsString(event.getDetail()));
        }
    }

    @Tracing
    void saveContractStatus(String propertyId, String contractStatus, String contractId, Long contractLastModifiedOn) {
        if (TABLE_NAME == null || TABLE_NAME.isEmpty()) {
            throw new RuntimeException("CONTRACT_STATUS_TABLE environment variable is not set");
        }
        
        logger.info("Updating DynamoDB table: {} for property: {}", TABLE_NAME, propertyId);
        
        Map<String, AttributeValue> key = Map.of("property_id", AttributeValue.fromS(propertyId));
        
        Map<String, AttributeValue> expressionAttributeValues = Map.of(
            ":t", AttributeValue.fromS(contractStatus),
            ":c", AttributeValue.fromS(contractId),
            ":m", AttributeValue.fromN(String.valueOf(contractLastModifiedOn))
        );

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
            .key(key)
            .tableName(TABLE_NAME)
            .updateExpression("set contract_status=:t, contract_last_modified_on=:m, contract_id=:c")
            .expressionAttributeValues(expressionAttributeValues)
            .build();

        dynamodbClient.updateItem(updateItemRequest);
        logger.info("DynamoDB update completed for property: {}", propertyId);
    }

    public void setDynamodbClient(DynamoDbClient dynamodbClient) {
        this.dynamodbClient = dynamodbClient;
    }
}
