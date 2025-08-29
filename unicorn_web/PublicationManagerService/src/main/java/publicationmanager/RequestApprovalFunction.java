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
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
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
    @Logging(logEvent = true)
    public void handleRequest(final SQSEvent input, final Context context) {
        logger.info("Environment variables - DYNAMODB_TABLE: {}, EVENT_BUS: {}", tableName, eventBus);
        logger.info("Starting approval request processing for {} messages", input.getRecords().size());

        
        for (SQSEvent.SQSMessage message : input.getRecords()) {
            try {
                String body = message.getBody();
                logger.info("Processing SQS message: {}", body);
                
                if (body == null || body.trim().isEmpty()) {
                    logger.warn("Message body is null or empty");
                    continue;
                }

                JsonNode rootNode = objectMapper.readTree(body);
                JsonNode propertyIdNode = rootNode.get("property_id");
                
                if (propertyIdNode == null) {
                    logger.warn("property_id field missing from message");
                    continue;
                }

                String propertyId = propertyIdNode.asText();
                logger.info("Processing approval request for property: {}", propertyId);
                
                if (!propertyIdPattern.matcher(propertyId).matches()) {
                    logger.warn("Invalid property_id format: {}", propertyId);
                    continue;
                }

                PropertyComponents components = parsePropertyId(propertyId);
                logger.info("Parsed property ID components: {}", components);
                List<Property> properties = queryTable(components.partitionKey, components.sortKey);
                
                if (properties.isEmpty()) {
                    logger.warn("Property not found in database: {}", propertyId);
                    continue;
                }

                Property property = properties.get(0);
                logger.info("Found property with status: {}", property.getStatus());
                
                if (NO_ACTION_STATUSES.contains(property.getStatus())) {
                    logger.info("Property already approved, no action needed: {}", propertyId);
                    continue;
                }

                sendEvent(property);
                logger.info("Approval request completed successfully for property: {}", propertyId);

            } catch (JsonProcessingException e) {
                logger.error("Invalid JSON in message body: {}", message.getBody(), e);
            } catch (Exception e) {
                logger.error("Error processing approval request for message: {}", message.getBody(), e);
            }
        }
    }

    private PropertyComponents parsePropertyId(String propertyId) {
        String[] parts = propertyId.split("/");
        String partitionKey = ("PROPERTY#" + parts[0] + "#" + parts[1]).replace(' ', '-');
        String sortKey = (parts[2] + "#" + parts[3]).replace(' ', '-').toLowerCase();
        return new PropertyComponents(partitionKey, sortKey);
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
        logger.info("Starting DynamoDB query with partitionKey: {}, sortKey: {}", partitionKey, sortKey);
        
        if (partitionKey == null || sortKey == null) {
            logger.error("Null keys provided - partitionKey: {}, sortKey: {}", partitionKey, sortKey);
            throw new IllegalArgumentException("Partition key and sort key cannot be null");
        }

        List<Property> result = new ArrayList<>();
        Key key = Key.builder().partitionValue(partitionKey).sortValue(sortKey).build();
        QueryConditional queryConditional = QueryConditional.sortBeginsWith(key);
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        try {
            logger.debug("Executing DynamoDB query on table: {}", tableName);
            SdkPublisher<Property> properties = propertyTable.query(request).items();
            CompletableFuture<Void> future = properties.subscribe(result::add);
            future.get();
            logger.info("DynamoDB query completed successfully, found {} properties", result.size());
            return result;
        } catch (DynamoDbException | InterruptedException | ExecutionException e) {
            logger.error("Error querying DynamoDB with partitionKey: {}, sortKey: {}, table: {}", 
                        partitionKey, sortKey, tableName, e);
            throw new Exception("Database query failed: " + e.getMessage());
        }
    }

    @Tracing
    @Metrics
    private void sendEvent(Property property) throws JsonProcessingException {
        logger.info("Creating approval event for property: {}", property.getId());
        
        RequestApproval event = new RequestApproval();
        event.setPropertyId(property.getId());
        
        Address address = new Address();
        address.setCity(property.getCity());
        address.setCountry(property.getCountry());
        address.setNumber(property.getPropertyNumber());
        event.setAddress(address);

        String eventString = objectMapper.writeValueAsString(event);
        logger.info("Event payload created: {}", eventString);

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

        logger.debug("Sending event to EventBridge bus: {}", eventBus);
        try {
            eventBridgeClient.putEvents(eventsRequest).join();
            logger.info("Event sent successfully for property: {}", property.getId());
        } catch (Exception e) {
            logger.error("Failed to send event to EventBridge for property: {}, bus: {}", 
                        property.getId(), eventBus, e);
            throw e;
        }
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
