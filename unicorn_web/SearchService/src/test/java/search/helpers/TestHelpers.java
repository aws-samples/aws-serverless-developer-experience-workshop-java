package search.helpers;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Map;

/**
 * Test helper utilities for building API Gateway proxy request events
 * used by the Search service tests.
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
}
