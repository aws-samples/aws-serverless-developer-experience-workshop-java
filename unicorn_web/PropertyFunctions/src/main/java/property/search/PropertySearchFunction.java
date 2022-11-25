package property.search;

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

import property.dao.Property;
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
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;

/**
 * Handler for requests to Lambda function.
 */
public class PropertySearchFunction
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(PropertySearchFunction.class);

    String TABLE_NAME = System.getenv("DYNAMODB_TABLE");

    DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
            .httpClientBuilder(NettyNioAsyncHttpClient.builder())
            .build();

    DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(dynamodbClient)
            .build();

    DynamoDbAsyncTable<Property> propertyTable = enhancedClient.table(TABLE_NAME,
            TableSchema.fromBean(Property.class));

    final String SERVICE_NAME = System.getenv("POWERTOOLS_SERVICE_NAME");
    final String METRICS_NAMESPACE = System.getenv("POWERTOOLS_METRICS_NAMESPACE");
    final String EVENT_BUS = System.getenv("EVENT_BUS");

    MetricsLogger metricsLogger = MetricsUtils.metricsLogger();
    ObjectMapper objectMapper = new ObjectMapper();

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true, correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        String method = input.getHttpMethod();
        if (!method.equalsIgnoreCase("get")) {
            return response
                    .withStatusCode(400)
                    .withBody("{ \"message\": \"ErrorInRequest\",  \"requestdetails\": \"Input Invalid\" }");
        }
        String requestPath = input.getResource();
        String responseString = null;
        String strPartitionKey = ("property#" + input.getPathParameters().get("country") + "#"
                + input.getPathParameters().get("city")).replace(' ', '-').toLowerCase();

        String strSortKey = null;
        switch (requestPath) {
            case "/search/{country}/{city}":
                // code to call
                logger.info("path is " + requestPath);

                try {
                    List<Property> result = queryTable(strPartitionKey, null);
                    responseString = objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    return response
                            .withStatusCode(500)
                            .withBody(
                                    "{ \"message\": \"ErrorInRequest\",  \"requestdetails\": \"Cannot Process Request\" }");
                }
                break;
            case "/search/{country}/{city}/{street}":
                // code to call
                logger.info("path is " + requestPath);
                logger.info("path is " + requestPath);
                strSortKey = input.getPathParameters().get("street");
                strSortKey = strSortKey.replace(' ', '-').toLowerCase();

                try {
                    List<Property> result = queryTable(strPartitionKey, strSortKey);
                    responseString = objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    return response
                            .withStatusCode(500)
                            .withBody(
                                    "{ \"message\": \"ErrorInRequest\",  \"requestdetails\": \"Cannot Process Request\" }");
                }
                break;
            case "/properties/{country}/{city}/{street}/{number}":
                logger.info("path is " + requestPath);
                logger.info("path is " + requestPath);
                strSortKey = input.getPathParameters().get("street") + "#" + input.getPathParameters().get("number");
                strSortKey = strSortKey.replace(' ', '-').toLowerCase();

                try {
                    List<Property> result = queryTable(strPartitionKey, strSortKey);
                    responseString = objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    return response
                            .withStatusCode(500)
                            .withBody(
                                    "{ \"message\": \"ErrorInRequest\",  \"requestdetails\": \"Cannot Process Request\" }");
                }
                break;
            default:
                return response
                        .withStatusCode(400)
                        .withBody("{ \"message\": \"ErrorInRequest\",  \"requestdetails\": \"Input Invalid\" }");

        }

        return response
                .withStatusCode(200)
                .withBody(String.format("{ \"message\": \"Properties\",  \"requestdetails\": \"%s\" }",
                        responseString));
    }

    public List<Property> queryTable(String partitionkey, String sortKey) throws Exception {

        try {
            if (partitionkey == null) {
                throw new Exception("Invalid Input");
            }
            List<Property> result = new ArrayList<Property>();
            SdkPublisher<Property> properties = null;

            AttributeValue attributeValue = AttributeValue.builder()
                    .s("APPROVED")
                    .build();
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":value", attributeValue);

            Map<String, String> expressionNames = new HashMap<>();
            expressionNames.put("#property_status", "status");

            Expression expression = Expression.builder()
                    .expressionNames(expressionNames)
                    .expression("#property_status = :value")
                    .expressionValues(expressionValues)
                    .build();

            if (sortKey != null) {
                Key key = Key.builder().partitionValue(partitionkey).sortValue(sortKey).build();

                QueryConditional queryConditional = QueryConditional.sortBeginsWith(key);
                QueryEnhancedRequest request = QueryEnhancedRequest.builder().queryConditional(queryConditional)
                        .filterExpression(expression).build();
                properties = propertyTable.query(request).items();

            } else {
                Key key = Key.builder().partitionValue(partitionkey).build();
                QueryConditional queryConditional = QueryConditional.keyEqualTo(key);
                QueryEnhancedRequest request = QueryEnhancedRequest.builder().queryConditional(queryConditional)
                        .filterExpression(expression).build();
                properties = propertyTable.query(request).items();
            }

            CompletableFuture<Void> future = properties.subscribe(res -> {
                // Add response to the list
                result.add(res);
            });
            future.get();

            return result;

        } catch (DynamoDbException | InterruptedException | ExecutionException e) {
            throw new Exception(e.getMessage());
        }
    }

}
