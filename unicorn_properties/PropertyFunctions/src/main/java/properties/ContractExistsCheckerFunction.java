package properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import properties.helper.PropertyHelper;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

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

    PropertyHelper helper = new PropertyHelper(TABLE_NAME);

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true)
    public void handleRequest(InputStream inputStream, OutputStream outputStream,
            Context context) throws IOException, ContractStatusNotFoundException {

        ObjectMapper objectMapper = new ObjectMapper();
        String input = new String(inputStream.readAllBytes());
        JsonNode rootNode = objectMapper.readTree(input);

        String property_id = rootNode.path("Input").get("property_id").asText();

        try {

            Map contractMap = getContractStatus(property_id);
            if (getContractStatus(property_id).isEmpty()) {
                throw new ContractStatusNotFoundException("Contract for property " + property_id + " not found");
            } else {
                String responseString = getContractStatus(property_id).toString();
                OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                writer.write(responseString);
                writer.close();
            }

        } catch (DynamoDbException e) {
            throw new ContractStatusNotFoundException(e.getLocalizedMessage());
        }

    }

    @Tracing
    Map getContractStatus(String property_id) {
        return helper.getContractStatus(property_id);
    }

    public void setHelper(PropertyHelper helper) {
        this.helper = helper;
    }

}
