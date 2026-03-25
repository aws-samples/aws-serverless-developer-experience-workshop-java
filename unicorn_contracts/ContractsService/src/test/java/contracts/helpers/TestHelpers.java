package contracts.helpers;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Test helper utilities for building SQS events used by the Contracts service tests.
 */
public final class TestHelpers {

    private TestHelpers() {
        // Utility class
    }

    /**
     * Loads a JSON event file from src/test/events/ and returns its content as a String.
     *
     * @param name the file name without extension (e.g. "create_contract_valid_1")
     * @return the file content as a String
     */
    public static String loadEvent(String name) {
        String path = "src/test/events/" + name + ".json";
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read event file: " + path, e);
        }
    }

    /**
     * Creates an SQS event with an HttpMethod message attribute and a JSON body.
     *
     * @param httpMethod the HTTP method (e.g. POST, PUT)
     * @param body       the JSON body of the message
     * @return a fully-formed SQSEvent with one record
     */
    public static SQSEvent createSqsEvent(String httpMethod, String body) {
        SQSEvent event = new SQSEvent();
        SQSMessage message = new SQSMessage();
        message.setMessageId("test-message-id");
        message.setBody(body);

        MessageAttribute httpMethodAttr = new MessageAttribute();
        httpMethodAttr.setStringValue(httpMethod);
        message.setMessageAttributes(Map.of("HttpMethod", httpMethodAttr));

        event.setRecords(Collections.singletonList(message));
        return event;
    }

    /**
     * Creates an SQS event without the HttpMethod message attribute.
     *
     * @param body the JSON body of the message
     * @return an SQSEvent with one record that has no HttpMethod attribute
     */
    public static SQSEvent createSqsEventWithoutHttpMethod(String body) {
        SQSEvent event = new SQSEvent();
        SQSMessage message = new SQSMessage();
        message.setMessageId("test-message-id");
        message.setBody(body);
        message.setMessageAttributes(Collections.emptyMap());

        event.setRecords(Collections.singletonList(message));
        return event;
    }
}
