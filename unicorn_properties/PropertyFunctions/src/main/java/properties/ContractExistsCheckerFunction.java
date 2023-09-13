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

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

/**
 * Function checks for the existence of a contract status entry for a specified
 * property.
 * 
 * If an entry exists, pause the workflow, and update the record with task
 * token.
 */
public class ContractExistsCheckerFunction {

    Logger logger = LogManager.getLogger();

    final String TABLE_NAME = System.getenv("CONTRACT_STATUS_TABLE");

    DynamoDbClient dynamodbClient = DynamoDbClient.builder()
            .build();

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true)
    public void handleRequest(InputStream inputStream, OutputStream outputStream,
            Context context) throws IOException, ContractStatusNotFoundException {

        ObjectMapper objectMapper = new ObjectMapper();
        String input = new String(inputStream.readAllBytes());
        JsonNode rootNode = objectMapper.readTree(input);

        String property_id = rootNode.path("Input").get("property_id").asText();

        Map<String, AttributeValue> contractMap = getContractStatus(property_id);
        if (getContractStatus(property_id).isEmpty()) {
            throw new ContractStatusNotFoundException("Contract for property " + property_id + " not found");
        } else {
            String responseString = getContractStatus(property_id).toString();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            writer.write(responseString);
            writer.close();
        }

    }

    @Tracing
    Map<String, AttributeValue> getContractStatus(String property_id) throws ContractStatusNotFoundException {
        Map<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();
        keyToGet.put("property_id", AttributeValue.builder()
                .s(property_id).build());

        GetItemRequest request = GetItemRequest.builder()
                .key(keyToGet)
                .tableName(TABLE_NAME)
                .build();
        try {
            return dynamodbClient.getItem(request).item();
        } catch (ResourceNotFoundException exception) {
            throw new ContractStatusNotFoundException(exception.getMessage());
        }
    }

    public void setDynamodbClient(DynamoDbClient dynamodbClient) {
        this.dynamodbClient = dynamodbClient;
    }

}
