package property.requestapproval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

/**
 * Validates the integrity of the property content
 */
public class RequestApprovalFunction {

    Logger logger = LogManager.getLogger();
    Set<String> noActionSet = new HashSet<String>(Arrays.asList("APPROVED"));
    String SERVICE = "Unicorn.Web";
    String EXPRESSION = "[a-z-]+\\/[a-z-]+\\/[a-z][a-z0-9-]*\\/[0-9-]+";
    String TARGET_STATE = "PENDING";
    Pattern pattern = Pattern.compile(EXPRESSION);

    String TABLE_NAME = System.getenv("DYNAMODB_TABLE");
    String EVENT_BUS = System.getenv("EVENT_BUS");

    DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
            .httpClientBuilder(NettyNioAsyncHttpClient.builder())
            .build();

    DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(dynamodbClient)
            .build();

    DynamoDbAsyncTable<Property> propertyTable = enhancedClient.table(TABLE_NAME,
            TableSchema.fromBean(Property.class));

    EventBridgeAsyncClient eventBridgeClient = EventBridgeAsyncClient.builder()
            .httpClientBuilder(NettyNioAsyncHttpClient.builder())
            .build();

    ObjectMapper objectMapper = new ObjectMapper();

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true, correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input,
            final Context context) throws JsonMappingException, JsonProcessingException {
        {

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("X-Custom-Header", "application/json");
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                    .withHeaders(headers);

            JsonNode rootNode = objectMapper.readTree(input.getBody());
            String propertyId = rootNode.get("property_id").asText();
            Matcher matcher = pattern.matcher(propertyId);
            boolean valid = matcher.matches();
            if (!valid) {
                APIGatewayProxyResponseEvent errorResponse = response
                        .withBody("Input invalid; must conform to regular expression: " + EXPRESSION)
                        .withStatusCode(500);
                return errorResponse;
            }
            String[] splitString = propertyId.split("/");
            String country = splitString[0];
            String city = splitString[1];
            String street = splitString[2];
            String number = splitString[3];
            String strPartionKey = ("property#" + country + "#" + city).replace(' ', '-').toLowerCase();
            String strSortKey = (street + "#" + number).replace(' ', '-').toLowerCase();
            try {
                List<Property> properties = queryTable(strPartionKey, strSortKey);
                if (properties.size() <= 0) {
                    APIGatewayProxyResponseEvent errorResponse = response
                            .withBody("No property found in database with the requested property id")
                            .withStatusCode(500);
                    return errorResponse;
                }
                Property property = properties.get(0);
                if (noActionSet.contains(property.getStatus())) {
                    return response
                            .withStatusCode(200)
                            .withBody("'result': 'Property is already " + property.getStatus() + "; no action taken'");
                }
                sendEvent(property);

            } catch (Exception e) {
                APIGatewayProxyResponseEvent errorResponse = response
                        .withBody("Error in searching")
                        .withStatusCode(500);
                return errorResponse;
            }
            return response
                    .withStatusCode(200)
                    .withBody("'result': 'Approval Requested'");

        }

    }

    public List<Property> queryTable(String partitionkey, String sortKey) throws Exception {

        try {
            if (partitionkey == null || sortKey == null) {
                throw new Exception("Invalid Input");
            }
            List<Property> result = new ArrayList<Property>();
            SdkPublisher<Property> properties = null;

            Key key = Key.builder().partitionValue(partitionkey).sortValue(sortKey).build();

            QueryConditional queryConditional = QueryConditional.sortBeginsWith(key);
            QueryEnhancedRequest request = QueryEnhancedRequest.builder().queryConditional(queryConditional)
                    .build();
            properties = propertyTable.query(request).items();

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

    @Tracing
    @Metrics
    public String sendEvent(Property property)
            throws JsonProcessingException {

        RequestApproval event = new RequestApproval();
        event.setPropertyId(property.getId());
        Address address = new Address();
        address.setCity(property.getCity());
        address.setCountry(property.getCountry());
        address.setNumber(property.getPropertyNumber());
        event.setAddress(address);

        String event_string = objectMapper.writeValueAsString(event);

        List<PutEventsRequestEntry> requestEntries = new ArrayList<PutEventsRequestEntry>();

        requestEntries.add(PutEventsRequestEntry.builder()
                .eventBusName(EVENT_BUS)
                .source("Unicorn.Web")
                .resources(property.getId())
                .detailType("PublicationApprovalRequested")
                .detail(event_string).build());

        PutEventsRequest eventsRequest = PutEventsRequest.builder().entries(requestEntries).build();

        eventBridgeClient.putEvents(eventsRequest).join();

        return event_string;
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

    String city;
    String state;
    String number;

}
