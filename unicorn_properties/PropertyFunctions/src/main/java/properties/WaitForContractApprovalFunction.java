package properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

/**
 * Lambda handler to update the contract status change
 */
public class WaitForContractApprovalFunction {

        Logger logger = LogManager.getLogger();

        final String TABLE_NAME = System.getenv("CONTRACT_STATUS_TABLE");

        DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
                        .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                        .maxConcurrency(100)
                                        .maxPendingConnectionAcquires(10_000))
                        .build();

        @Tracing
        @Metrics(captureColdStart = true)
        @Logging(logEvent = true)
        public void handleRequest(InputStream inputStream, OutputStream outputStream,
                        Context context) throws IOException, ContractStatusNotFoundException {

                // deseralised to contract status
                ObjectMapper objectMapper = new ObjectMapper();
                String srtInput = new String(inputStream.readAllBytes());
                JsonNode event = objectMapper.readTree(srtInput);
                String propertyId = event.get("Input").get("property_id").asText();
                String taskToken = event.get("TaskToken").asText();

                logger.info("task Token : ", taskToken);
                logger.info("Property Id : ", propertyId);

                // get contract status
                Map<String, AttributeValue> dynamodbItem = getContractStatus(propertyId);
                updateTokenAndPauseExecution(taskToken, dynamodbItem.get("property_id").s());

                String responseString = event.get("Input").asText();
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                logger.debug(responseString);
                writer.write(responseString);
                writer.close();

        }

        private void updateTokenAndPauseExecution(String taskToken, String propertyId) {
                Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
                AttributeValue keyvalue = AttributeValue.fromS(propertyId);
                key.put("property_id", keyvalue);

                Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
                expressionAttributeValues.put(":g", AttributeValue.fromS(taskToken));

                UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                                .key(key)
                                .tableName(TABLE_NAME)
                                .updateExpression(
                                                "set sfn_wait_approved_task_token = :g")
                                .expressionAttributeValues(expressionAttributeValues)
                                .build();
                dynamodbClient.updateItem(updateItemRequest).join();
        }

        private Map<String, AttributeValue> getContractStatus(String propertyId)
                        throws ContractStatusNotFoundException {
                HashMap<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();

                keyToGet.put("property_id", AttributeValue.builder()
                                .s(propertyId).build());

                GetItemRequest request = GetItemRequest.builder()
                                .key(keyToGet)
                                .tableName(TABLE_NAME)
                                .build();
                Map<String, AttributeValue> returnvalue = null;
                try {
                        returnvalue = dynamodbClient.getItem(request).join().item();
                } catch (Exception exception) {
                        throw new ContractStatusNotFoundException(exception.getLocalizedMessage());
                }

                return returnvalue;
        }

}
