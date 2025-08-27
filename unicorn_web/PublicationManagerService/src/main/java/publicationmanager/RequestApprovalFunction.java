package publicationmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.Property;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

/**
 * Validates property requests and sends approval events.
 */
public class RequestApprovalFunction {

    private static final Logger logger = LogManager.getLogger(RequestApprovalFunction.class);
    private static final Set<String> NO_ACTION_STATUSES = new HashSet<>(Arrays.asList("APPROVED"));
    private static final String PROPERTY_ID_PATTERN = "[a-z-]+\\/[a-z-]+\\/[a-z][a-z0-9-]*\\/[0-9-]+";
    private static final String CONTENT_TYPE = "application/json";
    
    private final Pattern propertyIdPattern = Pattern.compile(PROPERTY_ID_PATTERN);
    private final String tableName = System.getenv("DYNAMODB_TABLE");
    private final String eventBus = System.getenv("EVENT_BUS");
    
    private final DynamoDbAsyncTable<Property> propertyTable;
    private final EventBridgeAsyncClient eventBridgeClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequestApprovalFunction() {
        DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                .build();

        DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamodbClient)
                .build();

        this.propertyTable = enhancedClient.table(tableName, TableSchema.fromBean(Property.class));
        this.eventBridgeClient = EventBridgeAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                .build();
    }

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true, correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input,
            final Context context) {
        try {
            if (input.getBody() == null || input.getBody().trim().isEmpty()) {
                return createErrorResponse(400, "Request body is required");
            }

            JsonNode rootNode = objectMapper.readTree(input.getBody());
            JsonNode propertyIdNode = rootNode.get("property_id");
            
            if (propertyIdNode == null) {
                return createErrorResponse(400, "property_id field is required");
            }

            String propertyId = propertyIdNode.asText();
            if (!propertyIdPattern.matcher(propertyId).matches()) {
                return createErrorResponse(400, "Invalid property_id format. Must match: " + PROPERTY_ID_PATTERN);
            }

            PropertyComponents components = parsePropertyId(propertyId);
            List<Property> properties = queryTable(components.partitionKey, components.sortKey);
            
            if (properties.isEmpty()) {
                return createErrorResponse(404, "Property not found");
            }

            Property property = properties.get(0);
            if (NO_ACTION_STATUSES.contains(property.getStatus())) {
                return createSuccessResponse("Property is already " + property.getStatus() + "; no action taken");
            }

            sendEvent(property);
            return createSuccessResponse("Approval requested successfully");

        } catch (JsonProcessingException e) {
            logger.error("Invalid JSON in request body", e);
            return createErrorResponse(400, "Invalid JSON format");
        } catch (Exception e) {
            logger.error("Error processing approval request", e);
            return createErrorResponse(500, "Internal server error");
        }
    }

    private PropertyComponents parsePropertyId(String propertyId) {
        String[] parts = propertyId.split("/");
        String partitionKey = ("search#" + parts[0] + "#" + parts[1]).replace(' ', '-').toLowerCase();
        String sortKey = (parts[2] + "#" + parts[3]).replace(' ', '-').toLowerCase();
        return new PropertyComponents(partitionKey, sortKey);
    }

    private APIGatewayProxyResponseEvent createSuccessResponse(String message) {
        String body = String.format("{\"result\":\"%s\"}", message);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of("Content-Type", CONTENT_TYPE))
                .withBody(body);
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        String body = String.format("{\"error\":\"%s\"}", message);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", CONTENT_TYPE))
                .withBody(body);
    }

    private static class PropertyComponents {
        final String partitionKey;
        final String sortKey;

        PropertyComponents(String partitionKey, String sortKey) {
            this.partitionKey = partitionKey;
            this.sortKey = sortKey;
        }
    }

    private List<Property> queryTable(String partitionKey, String sortKey) throws Exception {
        if (partitionKey == null || sortKey == null) {
            throw new IllegalArgumentException("Partition key and sort key cannot be null");
        }

        List<Property> result = new ArrayList<>();
        Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();
        QueryConditional queryConditional = QueryConditional.sortBeginsWith(key);
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
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

    @Tracing
    @Metrics
    private void sendEvent(Property property) throws JsonProcessingException {
        RequestApproval event = new RequestApproval();
        event.setPropertyId(property.getId());
        
        Address address = new Address();
        address.setCity(property.getCity());
        address.setCountry(property.getCountry());
        address.setNumber(property.getPropertyNumber());
        event.setAddress(address);

        String eventString = objectMapper.writeValueAsString(event);

        PutEventsRequestEntry requestEntry = PutEventsRequestEntry.builder()
                .eventBusName(eventBus)
                .source("Unicorn.Web")
                .resources(property.getId())
                .detailType("PublicationApprovalRequested")
                .detail(eventString)
                .build();

        PutEventsRequest eventsRequest = PutEventsRequest.builder()
                .entries(requestEntry)
                .build();

        eventBridgeClient.putEvents(eventsRequest).join();
        logger.info("Event sent successfully for property: {}", property.getId());
    }
}

class RequestApproval {
    @JsonProperty("property_id")
    String propertyId;
    Address address;

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}

class Address {
    String country;
    String city;
    String state;
    String number;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
