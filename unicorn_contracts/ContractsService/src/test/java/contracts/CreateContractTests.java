package contracts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import contracts.helpers.TestHelpers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateContractTests {

    @Mock
    private Context context;

    @Mock
    private DynamoDbClient dynamoDbClient;

    // yyyy-MM-ddTHH:mm:ssZ: UTC, whole seconds, literal Z (20 characters)
    private static final String TIMESTAMP_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private ContractEventHandlerFunction handler;

    @BeforeEach
    public void setUp() {
        handler = new ContractEventHandlerFunction(dynamoDbClient);
    }

    @Test
    public void shouldProcessValidCreateEvent() {
        // Given
        String payload = TestHelpers.loadEvent("create_contract_valid_1");
        SQSEvent event = TestHelpers.createSqsEvent("POST", payload);

        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // When
        handler.handleRequest(event, context);

        // Then
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient, times(1)).putItem(captor.capture());

        String created = captor.getValue().item().get("contract_created").s();
        String lastModified = captor.getValue().item().get("contract_last_modified_on").s();
        assertTrue(created.matches(TIMESTAMP_PATTERN), "contract_created was " + created);
        assertTrue(lastModified.matches(TIMESTAMP_PATTERN), "contract_last_modified_on was " + lastModified);
        // A new contract has one creation instant
        assertEquals(created, lastModified);
    }

    @Test
    public void shouldProcessValidUpdateEvent() {
        // Given
        String payload = TestHelpers.loadEvent("update_contract_valid_1");
        SQSEvent event = TestHelpers.createSqsEvent("PUT", payload);

        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        // When
        handler.handleRequest(event, context);

        // Then
        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(dynamoDbClient, times(1)).updateItem(captor.capture());
        verify(dynamoDbClient, never()).putItem(any(PutItemRequest.class));

        String modified = captor.getValue().expressionAttributeValues().get(":modifiedDate").s();
        assertTrue(modified.matches(TIMESTAMP_PATTERN), ":modifiedDate was " + modified);
    }

    @Test
    public void shouldHandleConditionalCheckFailedOnCreate() {
        // Given
        String payload = TestHelpers.loadEvent("create_contract_valid_1");
        SQSEvent event = TestHelpers.createSqsEvent("POST", payload);

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
        String payload = TestHelpers.loadEvent("update_contract_valid_1");
        SQSEvent event = TestHelpers.createSqsEvent("PUT", payload);

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
        SQSEvent event = TestHelpers.createSqsEvent("POST", "{ this is not valid json }");

        // When / Then
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, context));
        verifyNoInteractions(dynamoDbClient);
    }

    @Test
    public void shouldIgnoreUnsupportedHttpMethod() {
        // Given
        String payload = TestHelpers.loadEvent("create_contract_valid_1");
        SQSEvent event = TestHelpers.createSqsEvent("DELETE", payload);

        // When / Then - DELETE is not handled, no DynamoDB interaction
        assertDoesNotThrow(() -> handler.handleRequest(event, context));
        verifyNoInteractions(dynamoDbClient);
    }

}
