package approvals;

import approvals.helpers.TestHelpers;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sfn.SfnAsyncClient;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessResponse;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PropertiesApprovalSyncFunctionTests {

    @Mock
    private Context context;

    @Mock
    private SfnAsyncClient sfnAsyncClient;

    private PropertiesApprovalSyncFunction handler;

    @BeforeEach
    public void setUp() throws Exception {
        handler = new PropertiesApprovalSyncFunction();
        // Inject the mock SFN client via reflection
        Field sfnField = PropertiesApprovalSyncFunction.class.getDeclaredField("sfnClient");
        sfnField.setAccessible(true);
        sfnField.set(handler, sfnAsyncClient);
    }

    @Test
    public void shouldSendTaskSuccessWhenApprovedWithToken() {
        // Given
        DynamodbEvent event = TestHelpers.createDynamoDbStreamEvent(
            "usa/anytown/main-street/111",
            "contract-001",
            "APPROVED",
            "DRAFT",
            "test-task-token-123",
            null
        );

        when(sfnAsyncClient.sendTaskSuccess(any(SendTaskSuccessRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(SendTaskSuccessResponse.builder().build()));

        // When
        StreamsEventResponse response = handler.handleRequest(event, context);

        // Then
        assertTrue(response.getBatchItemFailures() == null || response.getBatchItemFailures().isEmpty());
        verify(sfnAsyncClient, times(1)).sendTaskSuccess(any(SendTaskSuccessRequest.class));
    }

    @Test
    public void shouldSkipRecordWithoutTaskToken() {
        // Given - no task token in either NewImage or OldImage
        DynamodbEvent event = TestHelpers.createDynamoDbStreamEvent(
            "usa/anytown/main-street/111",
            "contract-001",
            "APPROVED",
            null,
            null,
            null
        );

        // When
        StreamsEventResponse response = handler.handleRequest(event, context);

        // Then
        assertTrue(response.getBatchItemFailures() == null || response.getBatchItemFailures().isEmpty());
        verifyNoInteractions(sfnAsyncClient);
    }

    @Test
    public void shouldSkipRecordWithNonApprovedStatus() {
        // Given - status is DRAFT, not APPROVED
        DynamodbEvent event = TestHelpers.createDynamoDbStreamEvent(
            "usa/anytown/main-street/111",
            "contract-001",
            "DRAFT",
            null,
            "test-task-token-123",
            null
        );

        // When
        StreamsEventResponse response = handler.handleRequest(event, context);

        // Then
        assertTrue(response.getBatchItemFailures() == null || response.getBatchItemFailures().isEmpty());
        verifyNoInteractions(sfnAsyncClient);
    }

    @Test
    public void shouldSkipRecordWithMissingNewImage() {
        // Given
        DynamodbEvent event = TestHelpers.createDynamoDbStreamEventWithNullNewImage();

        // When
        StreamsEventResponse response = handler.handleRequest(event, context);

        // Then
        assertTrue(response.getBatchItemFailures() == null || response.getBatchItemFailures().isEmpty());
        verifyNoInteractions(sfnAsyncClient);
    }
}
