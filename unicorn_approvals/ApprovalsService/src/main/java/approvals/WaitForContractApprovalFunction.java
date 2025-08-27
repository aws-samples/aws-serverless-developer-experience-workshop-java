package approvals;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletionException;

/**
 * Lambda handler to wait for contract approval in Step Functions workflow
 */
public class WaitForContractApprovalFunction {

    private static final Logger logger = LogManager.getLogger();
    private static final String TABLE_NAME = System.getenv("CONTRACT_STATUS_TABLE");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
        .httpClientBuilder(NettyNioAsyncHttpClient.builder()
            .maxConcurrency(100)
            .maxPendingConnectionAcquires(10_000))
        .build();

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true)
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) 
            throws IOException, ContractStatusNotFoundException {
        
        String input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        JsonNode event = objectMapper.readTree(input);
        
        String propertyId = event.get("Input").get("property_id").asText();
        String taskToken = event.get("TaskToken").asText();

        logger.info("Processing property: {} with task token: {}", propertyId, taskToken);

        Map<String, AttributeValue> contractItem = getContractStatus(propertyId);
        updateTokenAndPauseExecution(taskToken, contractItem.get("property_id").s());

        String responseString = event.get("Input").toString();
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(responseString);
        }
    }

    private void updateTokenAndPauseExecution(String taskToken, String propertyId) {
        Map<String, AttributeValue> key = Map.of("property_id", AttributeValue.fromS(propertyId));
        Map<String, AttributeValue> expressionAttributeValues = Map.of(":g", AttributeValue.fromS(taskToken));

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
            .key(key)
            .tableName(TABLE_NAME)
            .updateExpression("set sfn_wait_approved_task_token = :g")
            .expressionAttributeValues(expressionAttributeValues)
            .build();
            
        dynamodbClient.updateItem(updateItemRequest).join();
    }

    private Map<String, AttributeValue> getContractStatus(String propertyId) throws ContractStatusNotFoundException {
        Map<String, AttributeValue> key = Map.of("property_id", AttributeValue.fromS(propertyId));

        GetItemRequest request = GetItemRequest.builder()
            .key(key)
            .tableName(TABLE_NAME)
            .build();
            
        try {
            Map<String, AttributeValue> item = dynamodbClient.getItem(request).join().item();
            if (item == null || item.isEmpty()) {
                throw new ContractStatusNotFoundException("Contract status not found for property: " + propertyId);
            }
            return item;
        } catch (CompletionException e) {
            throw new ContractStatusNotFoundException("Failed to retrieve contract status: " + e.getCause().getMessage());
        }
    }
}
