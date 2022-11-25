package properties;

import com.amazonaws.services.lambda.runtime.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;

/**
 * Validates the integrity of the property content
 */
public class ValidateContentIntegrityFunction {

    Logger logger = LogManager.getLogger();

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true)
    public void handleRequest(InputStream inputStream, OutputStream outputStream,
            Context context) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String input = new String(inputStream.readAllBytes());
        String status = "PASS";
        JsonNode rootNode = objectMapper.readTree(input);

        JsonNode imageModeration = rootNode.path("imageModerations");

        Iterator<JsonNode> elementsIterator = imageModeration.elements();
        while (elementsIterator.hasNext()) {
            JsonNode imageModerationElement = elementsIterator.next();
            JsonNode moderationLabels = imageModerationElement.path("ModerationLabels");
            if (!moderationLabels.isEmpty()) {
                status = "FAIL";
                break;
            }

        }
        String sentiment = rootNode.path("contentSentiment").path("Sentiment").asText();
        if (!sentiment.equals("POSITIVE")) {
            status = "FAIL";
        }
        ((ObjectNode) rootNode).put("validation_result", status);
        String responseString = ((JsonNode) rootNode).toString();
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        logger.debug(responseString);
        writer.write(responseString);
        writer.close();
    }

}
