package search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.Property;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;

/**
 * Handler for property search requests.
 */
public class PropertySearchFunction
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(PropertySearchFunction.class);
    private static final String APPROVED_STATUS = "APPROVED";
    private static final String CONTENT_TYPE = "application/json";

    private final String tableName = System.getenv("DYNAMODB_TABLE");
    private final DynamoDbAsyncTable<Property> propertyTable;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PropertySearchFunction() {
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
    @Logging(logEvent = true, correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try {
            if (!"GET".equalsIgnoreCase(input.getHttpMethod())) {
                return createErrorResponse(400, "Method not allowed");
            }

            Map<String, String> pathParams = input.getPathParameters();
            if (pathParams == null || pathParams.get("country") == null || pathParams.get("city") == null) {
                return createErrorResponse(400, "Missing required path parameters");
            }

            String partitionKey = buildPartitionKey(pathParams.get("country"), pathParams.get("city"));
            String sortKey = buildSortKey(input.getResource(), pathParams);
            logger.info("Partition Key: {}, Sort Key: {}", partitionKey, sortKey);

            List<Property> properties = queryTable(partitionKey, sortKey);
            String responseBody = objectMapper.writeValueAsString(properties);

            return createSuccessResponse(responseBody);

        } catch (Exception e) {
            logger.error("Error processing request", e);
            return createErrorResponse(500, "Internal server error");
        }
    }

    private String buildPartitionKey(String country, String city) {
        return ("PROPERTY#" + country + "#" + city).replace(' ', '-');
    }

    private String buildSortKey(String resource, Map<String, String> pathParams) {
        switch (resource) {
            case "/search/{country}/{city}":
                return null;
            case "/search/{country}/{city}/{street}":
                return pathParams.get("street").replace(' ', '-').toLowerCase();
            case "/properties/{country}/{city}/{street}/{number}":
                return (pathParams.get("street") + "#" + pathParams.get("number")).replace(' ', '-').toLowerCase();
            default:
                throw new IllegalArgumentException("Unsupported resource path: " + resource);
        }
    }

    private APIGatewayProxyResponseEvent createSuccessResponse(String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of("Content-Type", CONTENT_TYPE))
                .withBody(body);
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        String errorBody = String.format("{\"error\":\"%s\"}", message);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", CONTENT_TYPE))
                .withBody(errorBody);
    }

    private List<Property> queryTable(String partitionKey, String sortKey) throws Exception {
        if (partitionKey == null) {
            throw new IllegalArgumentException("Partition key cannot be null");
        }

        List<Property> result = new ArrayList<>();
        
        Expression filterExpression = Expression.builder()
                .expressionNames(Map.of("#property_status", "status"))
                .expression("#property_status = :value")
                .expressionValues(Map.of(":value", AttributeValue.builder().s(APPROVED_STATUS).build()))
                .build();

        QueryConditional queryConditional;
        if (sortKey != null) {
            Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();
            queryConditional = QueryConditional.sortBeginsWith(key);
        } else {
            Key key = Key.builder().partitionValue(partitionKey).build();
            queryConditional = QueryConditional.keyEqualTo(key);
        }

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(filterExpression)
                .build();

        try {
            SdkPublisher<Property> properties = propertyTable.query(request).items();
            CompletableFuture<Void> future = properties.subscribe(result::add);
            future.get();
            return result;
        } catch (DynamoDbException | InterruptedException | ExecutionException e) {
            logger.error("Error querying DynamoDB", e);
            throw new Exception("Database query failed: " + e.getMessage());
        }
    }
}
