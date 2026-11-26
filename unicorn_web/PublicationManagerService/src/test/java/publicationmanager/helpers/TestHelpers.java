package publicationmanager.helpers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import java.util.Collections;
import java.util.Map;

/**
 * Test helper utilities for building API Gateway proxy request events and SQS events
 * used by the Publication Manager service tests.
 */
public final class TestHelpers {

    private TestHelpers() {
        // Utility class
    }

    /**
     * Creates an API Gateway proxy request event.
     *
     * @param httpMethod     the HTTP method (e.g. GET, POST)
     * @param resource       the API Gateway resource path template
     * @param pathParameters the path parameters map
     * @return an APIGatewayProxyRequestEvent
     */
    public static APIGatewayProxyRequestEvent createApiGatewayEvent(
            String httpMethod,
            String resource,
            Map<String, String> pathParameters) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod(httpMethod);
        event.setResource(resource);
        event.setPathParameters(pathParameters);
        return event;
    }

    /**
     * Creates an SQS event with a single message body.
     *
     * @param body the JSON body of the SQS message
     * @return an SQSEvent with one record
     */
    public static SQSEvent createSqsEvent(String body) {
        SQSEvent event = new SQSEvent();
        SQSMessage message = new SQSMessage();
        message.setMessageId("test-message-id");
        message.setBody(body);
        event.setRecords(Collections.singletonList(message));
        return event;
    }
}
