package publicationmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.Property;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;
import schema.unicorn_approvals.publicationevaluationcompleted.marshaller.Marshaller;
import schema.unicorn_approvals.publicationevaluationcompleted.AWSEvent;
import schema.unicorn_approvals.publicationevaluationcompleted.PublicationEvaluationCompleted;

/**
 * Processes publication evaluation completed events and updates property status.
 */
public class PublicationApprovedEventHandler {

    private static final Logger logger = LogManager.getLogger(PublicationApprovedEventHandler.class);
    
    private final String tableName = System.getenv("DYNAMODB_TABLE");
    private final DynamoDbAsyncTable<Property> propertyTable;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PublicationApprovedEventHandler() {
        DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                .build();

        DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamodbClient)
                .build();

        this.propertyTable = enhancedClient.table(tableName, TableSchema.fromBean(Property.class));
    }

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true)
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        try {
            AWSEvent<PublicationEvaluationCompleted> event = Marshaller.unmarshalEvent(inputStream,
                    PublicationEvaluationCompleted.class);

            if (event.getDetail() == null) {
                throw new IllegalArgumentException("Event detail is null");
            }

            String propertyId = event.getDetail().getPropertyId();
            String evaluationResult = event.getDetail().getEvaluationResult();

            if (propertyId == null || propertyId.trim().isEmpty()) {
                throw new IllegalArgumentException("Property ID is null or empty");
            }

            if (evaluationResult == null || evaluationResult.trim().isEmpty()) {
                throw new IllegalArgumentException("Evaluation result is null or empty");
            }

            updatePropertyStatus(evaluationResult, propertyId);

            String response = objectMapper.writeValueAsString(
                Map.of("result", "Successfully updated property status"));
            
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write(response);
            }

        } catch (Exception e) {
            logger.error("Error processing publication evaluation event", e);
            String errorResponse = objectMapper.writeValueAsString(
                Map.of("error", "Failed to process event: " + e.getMessage()));
            
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write(errorResponse);
            }
            throw new RuntimeException("Event processing failed", e);
        }
    }

    @Tracing
    private void updatePropertyStatus(String evaluationResult, String propertyId) {
        logger.info("Updating property status for property ID: {}", propertyId);
        logger.info("Evaluation result: {}", evaluationResult);
        try {
            String[] parts = propertyId.split("/");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid property ID format: " + propertyId);
            }

            String partitionKey = ("PROPERTY#" + parts[0] + "#" + parts[1]).replace(' ', '-');
            String sortKey = (parts[2] + "#" + parts[3]).replace(' ', '-').toLowerCase();

            logger.info("Paritition Key: {}", partitionKey);
            logger.info("Sort Key: {}", sortKey);

            Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();
            Property existingProperty = propertyTable.getItem(key).join();

            if (existingProperty == null) {
                logger.error("Property not found for ID: {}", propertyId);
                throw new RuntimeException("Property not found with ID: " + propertyId);
            }
            logger.info("Existing property: {}", existingProperty);
            
            existingProperty.setPropertyNumber(parts[3]);
            existingProperty.setStatus(evaluationResult);

            logger.info("Updating property {} with status: {}", propertyId, evaluationResult);
            propertyTable.putItem(existingProperty).join();
            
        } catch (Exception e) {
            logger.error("Failed to update property status for ID: {}", propertyId, e);
            throw new RuntimeException("Property update failed", e);
        }
    }
}
