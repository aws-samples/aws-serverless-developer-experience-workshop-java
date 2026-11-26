package publicationmanager;

import publicationmanager.helpers.TestHelpers;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import dao.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestApprovalFunctionTests {

    @Mock
    private Context context;

    @Mock
    private DynamoDbAsyncTable<Property> propertyTable;

    @Mock
    private EventBridgeAsyncClient eventBridgeClient;

    @SuppressWarnings("unchecked")
    @Mock
    private PagePublisher<Property> mockPagePublisher;

    @Mock
    private SdkPublisher<Property> mockItemsPublisher;

    private RequestApprovalFunction handler;

    @BeforeEach
    public void setUp() throws Exception {
        handler = new RequestApprovalFunction();
        // Inject mock table and EventBridge client via reflection
        Field tableField = RequestApprovalFunction.class.getDeclaredField("propertyTable");
        tableField.setAccessible(true);
        tableField.set(handler, propertyTable);

        Field ebField = RequestApprovalFunction.class.getDeclaredField("eventBridgeClient");
        ebField.setAccessible(true);
        ebField.set(handler, eventBridgeClient);
    }

    private void stubQueryReturning(Property... properties) {
        when(propertyTable.query(any(QueryEnhancedRequest.class))).thenReturn(mockPagePublisher);
        when(mockPagePublisher.items()).thenReturn(mockItemsPublisher);
        when(mockItemsPublisher.subscribe(any(Consumer.class))).thenAnswer(invocation -> {
            Consumer<Property> consumer = invocation.getArgument(0);
            for (Property p : properties) {
                consumer.accept(p);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private Property createTestProperty(String status) {
        Property property = new Property();
        property.setCountry("usa");
        property.setCity("anytown");
        property.setStreet("main-street");
        property.setPropertyNumber("123");
        property.setStatus(status);
        property.setDescription("A nice house");
        property.setCurrency("USD");
        property.setListprice(200000f);
        property.setImages(List.of("image1.jpg"));
        return property;
    }

    @Test
    public void shouldQueryPropertyAndPublishEvent() {
        // Given
        Property property = createTestProperty("PENDING");
        stubQueryReturning(property);

        SQSEvent event = TestHelpers.createSqsEvent("{\"property_id\":\"usa/anytown/main-street/123\"}");

        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(PutEventsResponse.builder().build()));

        // When
        handler.handleRequest(event, context);

        // Then
        verify(propertyTable).query(any(QueryEnhancedRequest.class));
        verify(eventBridgeClient).putEvents(any(PutEventsRequest.class));
    }

    @Test
    public void shouldSkipInvalidPropertyIdFormat() {
        // Given - property_id does not match the expected pattern
        SQSEvent event = TestHelpers.createSqsEvent("{\"property_id\":\"invalid-format\"}");

        // When
        handler.handleRequest(event, context);

        // Then - no DynamoDB query or EventBridge call should be made
        verifyNoInteractions(propertyTable);
        verifyNoInteractions(eventBridgeClient);
    }

    @Test
    public void shouldSkipAlreadyApprovedProperty() {
        // Given
        Property property = createTestProperty("APPROVED");
        stubQueryReturning(property);

        SQSEvent event = TestHelpers.createSqsEvent("{\"property_id\":\"usa/anytown/main-street/123\"}");

        // When
        handler.handleRequest(event, context);

        // Then - property is APPROVED so no event should be sent
        verify(propertyTable).query(any(QueryEnhancedRequest.class));
        verifyNoInteractions(eventBridgeClient);
    }

    @Test
    public void shouldHandlePropertyNotFound() {
        // Given
        stubQueryReturning(); // empty result

        SQSEvent event = TestHelpers.createSqsEvent("{\"property_id\":\"usa/anytown/main-street/999\"}");

        // When
        handler.handleRequest(event, context);

        // Then - no event should be published
        verify(propertyTable).query(any(QueryEnhancedRequest.class));
        verifyNoInteractions(eventBridgeClient);
    }

    @Test
    public void shouldHandleEventBridgeFailure() {
        // Given
        Property property = createTestProperty("PENDING");
        stubQueryReturning(property);

        SQSEvent event = TestHelpers.createSqsEvent("{\"property_id\":\"usa/anytown/main-street/123\"}");

        CompletableFuture<PutEventsResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("EventBridge unavailable"));

        when(eventBridgeClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(failedFuture);

        // When / Then - the handler catches the exception via the outer try/catch
        assertDoesNotThrow(() -> handler.handleRequest(event, context));
        verify(eventBridgeClient).putEvents(any(PutEventsRequest.class));
    }
}
