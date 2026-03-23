package contracts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateContractTests {

    @Mock
    private Context context;

    @Mock
    private DynamoDbClient dynamoDbClient;

    private ContractEventHandlerFunction handler;

    @BeforeEach
    public void setUp() {
        handler = new ContractEventHandlerFunction(dynamoDbClient);
    }

    @Test
    public void shouldProcessValidCreateEvent() {
        // Given
        SQSEvent event = createTestEvent("POST",
            "{ \"address\": { \"country\": \"USA\", \"city\": \"Anytown\", \"street\": \"Main Street\", \"number\": 123 }, \"seller_name\": \"John Smith\", \"property_id\": \"usa/anytown/main-street/123\"}");

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // When
        handler.handleRequest(event, context);

        // Then
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    private SQSEvent createTestEvent(String httpMethod, String body) {
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

    @Test
    public void shouldProcessValidUpdateEvent() {
        // Given
        SQSEvent event = createTestEvent("PUT",
            "{ \"property_id\": \"usa/anytown/main-street/123\" }");

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        // When
        handler.handleRequest(event, context);

        // Then
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
        verify(dynamoDbClient, never()).putItem(any(PutItemRequest.class));
    }

    @Test
    public void shouldHandleConditionalCheckFailedOnCreate() {
        // Given
        SQSEvent event = createTestEvent("POST",
            "{ \"address\": { \"country\": \"USA\", \"city\": \"Anytown\", \"street\": \"Main Street\", \"number\": 123 }, \"seller_name\": \"John Smith\", \"property_id\": \"usa/anytown/main-street/123\"}");

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder()
                    .message("Active contract already exists").build());

        // When / Then
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, context));
        verify(dynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    public void shouldHandleConditionalCheckFailedOnUpdate() {
        // Given
        SQSEvent event = createTestEvent("PUT",
            "{ \"property_id\": \"usa/anytown/main-street/123\" }");

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(ConditionalCheckFailedException.builder()
                    .message("Contract not in DRAFT status").build());

        // When / Then
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, context));
        verify(dynamoDbClient, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    public void shouldHandleMalformedJsonBody() {
        // Given
        SQSEvent event = createTestEvent("POST", "{ this is not valid json }");

        // When / Then
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, context));
        verifyNoInteractions(dynamoDbClient);
    }

    @Test
    public void shouldIgnoreUnsupportedHttpMethod() {
        // Given
        SQSEvent event = createTestEvent("DELETE",
            "{ \"address\": { \"country\": \"USA\", \"city\": \"Anytown\", \"street\": \"Main Street\", \"number\": 123 }, \"seller_name\": \"John Smith\", \"property_id\": \"usa/anytown/main-street/123\"}");

        // When / Then - DELETE is not handled, no DynamoDB interaction
        assertDoesNotThrow(() -> handler.handleRequest(event, context));
        verifyNoInteractions(dynamoDbClient);
    }

}
