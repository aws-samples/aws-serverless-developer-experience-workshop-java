package approvals;

import approvals.helpers.TestHelpers;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WaitForContractApprovalFunctionTests {

    @Mock
    private Context context;

    @Mock
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    private WaitForContractApprovalFunction handler;

    @BeforeEach
    public void setUp() throws Exception {
        handler = new WaitForContractApprovalFunction();
        // Inject the mock DynamoDB async client via reflection
        Field dynamoField = WaitForContractApprovalFunction.class.getDeclaredField("dynamodbClient");
        dynamoField.setAccessible(true);
        dynamoField.set(handler, dynamoDbAsyncClient);
    }

    @Test
    public void shouldStoreTaskTokenAndReturnInput() throws Exception {
        // Given
        String propertyId = "usa/anytown/main-street/123";
        String taskToken = "test-task-token-abc";
        String eventJson = TestHelpers.createStepFunctionsEvent(taskToken, propertyId);

        Map<String, AttributeValue> item = Map.of(
            "property_id", AttributeValue.fromS(propertyId),
            "contract_status", AttributeValue.fromS("DRAFT")
        );

        when(dynamoDbAsyncClient.getItem(any(GetItemRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                GetItemResponse.builder().item(item).build()));

        when(dynamoDbAsyncClient.updateItem(any(UpdateItemRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                UpdateItemResponse.builder().build()));

        ByteArrayInputStream inputStream = new ByteArrayInputStream(eventJson.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        handler.handleRequest(inputStream, outputStream, context);

        // Then
        String response = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains(propertyId));
        verify(dynamoDbAsyncClient).getItem(any(GetItemRequest.class));
        verify(dynamoDbAsyncClient).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    public void shouldThrowWhenContractNotFound() {
        // Given
        String propertyId = "usa/anytown/main-street/999";
        String taskToken = "test-task-token-xyz";
        String eventJson = TestHelpers.createStepFunctionsEvent(taskToken, propertyId);

        // Return empty item map to simulate contract not found
        when(dynamoDbAsyncClient.getItem(any(GetItemRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                GetItemResponse.builder().item(Collections.emptyMap()).build()));

        ByteArrayInputStream inputStream = new ByteArrayInputStream(eventJson.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When / Then
        assertThrows(ContractStatusNotFoundException.class, () ->
            handler.handleRequest(inputStream, outputStream, context));
    }

    @Test
    public void shouldHandleDynamoDbFailure() {
        // Given
        String propertyId = "usa/anytown/main-street/123";
        String taskToken = "test-task-token-fail";
        String eventJson = TestHelpers.createStepFunctionsEvent(taskToken, propertyId);

        CompletableFuture<GetItemResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
            DynamoDbException.builder().message("Service unavailable").build());

        when(dynamoDbAsyncClient.getItem(any(GetItemRequest.class)))
            .thenReturn(failedFuture);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(eventJson.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When / Then
        assertThrows(ContractStatusNotFoundException.class, () ->
            handler.handleRequest(inputStream, outputStream, context));
    }
}
