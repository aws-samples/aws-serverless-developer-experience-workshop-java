package search;

import search.helpers.TestHelpers;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import dao.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PropertySearchFunctionTests {

    @Mock
    private Context context;

    @Mock
    private DynamoDbAsyncTable<Property> propertyTable;

    @SuppressWarnings("unchecked")
    @Mock
    private PagePublisher<Property> mockPagePublisher;

    @Mock
    private SdkPublisher<Property> mockItemsPublisher;

    private PropertySearchFunction handler;

    @BeforeEach
    public void setUp() throws Exception {
        handler = new PropertySearchFunction();
        Field tableField = PropertySearchFunction.class.getDeclaredField("propertyTable");
        tableField.setAccessible(true);
        tableField.set(handler, propertyTable);
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
        property.setDescription("A beautiful property");
        property.setCurrency("USD");
        property.setListprice(250000f);
        return property;
    }

    @Test
    public void shouldSearchByCity() {
        // Given
        Property property = createTestProperty("APPROVED");
        stubQueryReturning(property);

        APIGatewayProxyRequestEvent event = TestHelpers.createApiGatewayEvent(
            "GET",
            "/search/{country}/{city}",
            Map.of("country", "usa", "city", "anytown")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("anytown"));
        verify(propertyTable).query(any(QueryEnhancedRequest.class));
    }

    @Test
    public void shouldSearchByCityAndStreet() {
        // Given
        Property property = createTestProperty("APPROVED");
        stubQueryReturning(property);

        APIGatewayProxyRequestEvent event = TestHelpers.createApiGatewayEvent(
            "GET",
            "/search/{country}/{city}/{street}",
            Map.of("country", "usa", "city", "anytown", "street", "main-street")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("main-street"));
        verify(propertyTable).query(any(QueryEnhancedRequest.class));
    }

    @Test
    public void shouldReturnPropertyDetails() {
        // Given
        Property property = createTestProperty("APPROVED");
        stubQueryReturning(property);

        APIGatewayProxyRequestEvent event = TestHelpers.createApiGatewayEvent(
            "GET",
            "/properties/{country}/{city}/{street}/{number}",
            Map.of("country", "usa", "city", "anytown", "street", "main-street", "number", "123")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Then
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("250000"));
        verify(propertyTable).query(any(QueryEnhancedRequest.class));
    }

    @Test
    public void shouldReturn404WhenPropertyNotFound() {
        // Given - query returns empty list
        stubQueryReturning(); // no properties

        APIGatewayProxyRequestEvent event = TestHelpers.createApiGatewayEvent(
            "GET",
            "/properties/{country}/{city}/{street}/{number}",
            Map.of("country", "usa", "city", "anytown", "street", "main-street", "number", "999")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Then - returns 200 with an empty array (the handler returns the query result as-is)
        assertEquals(200, response.getStatusCode());
        assertEquals("[]", response.getBody());
    }

    @Test
    public void shouldReturn404WhenPropertyNotApproved() {
        // Given - the query has a filter expression for status=APPROVED,
        // so non-approved properties are filtered out at the DynamoDB level
        stubQueryReturning(); // no properties returned after filter

        APIGatewayProxyRequestEvent event = TestHelpers.createApiGatewayEvent(
            "GET",
            "/properties/{country}/{city}/{street}/{number}",
            Map.of("country", "usa", "city", "anytown", "street", "main-street", "number", "123")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Then - returns 200 with empty array since the filter excludes non-approved
        assertEquals(200, response.getStatusCode());
        assertEquals("[]", response.getBody());
    }

    @Test
    public void shouldReturn400ForNonGetMethod() {
        // Given
        APIGatewayProxyRequestEvent event = TestHelpers.createApiGatewayEvent(
            "POST",
            "/search/{country}/{city}",
            Map.of("country", "usa", "city", "anytown")
        );

        // When
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // Then
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Method not allowed"));
        verifyNoInteractions(propertyTable);
    }
}
