package publicationmanager;

import com.amazonaws.services.lambda.runtime.Context;
import dao.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PublicationEvaluationEventHandlerTests {

    @Mock
    private Context context;

    @Mock
    private DynamoDbAsyncTable<Property> propertyTable;

    private PublicationEvaluationEventHandler handler;

    private static final String APPROVED_EVENT = "{\n" +
        "  \"version\": \"0\",\n" +
        "  \"id\": \"test-id\",\n" +
        "  \"detail-type\": \"PublicationEvaluationCompleted\",\n" +
        "  \"source\": \"unicorn-approvals\",\n" +
        "  \"account\": \"123456789012\",\n" +
        "  \"time\": \"2022-08-16T06:33:05Z\",\n" +
        "  \"region\": \"us-east-1\",\n" +
        "  \"resources\": [],\n" +
        "  \"detail\": {\n" +
        "    \"property_id\": \"usa/anytown/main-street/123\",\n" +
        "    \"evaluation_result\": \"APPROVED\"\n" +
        "  }\n" +
        "}";

    private static final String DECLINED_EVENT = "{\n" +
        "  \"version\": \"0\",\n" +
        "  \"id\": \"test-id\",\n" +
        "  \"detail-type\": \"PublicationEvaluationCompleted\",\n" +
        "  \"source\": \"unicorn-approvals\",\n" +
        "  \"account\": \"123456789012\",\n" +
        "  \"time\": \"2022-08-16T06:33:05Z\",\n" +
        "  \"region\": \"us-east-1\",\n" +
        "  \"resources\": [],\n" +
        "  \"detail\": {\n" +
        "    \"property_id\": \"usa/anytown/main-street/123\",\n" +
        "    \"evaluation_result\": \"DECLINED\"\n" +
        "  }\n" +
        "}";

    private static final String UNKNOWN_RESULT_EVENT = "{\n" +
        "  \"version\": \"0\",\n" +
        "  \"id\": \"test-id\",\n" +
        "  \"detail-type\": \"PublicationEvaluationCompleted\",\n" +
        "  \"source\": \"unicorn-approvals\",\n" +
        "  \"account\": \"123456789012\",\n" +
        "  \"time\": \"2022-08-16T06:33:05Z\",\n" +
        "  \"region\": \"us-east-1\",\n" +
        "  \"resources\": [],\n" +
        "  \"detail\": {\n" +
        "    \"property_id\": \"usa/anytown/main-street/123\",\n" +
        "    \"evaluation_result\": \"UNKNOWN\"\n" +
        "  }\n" +
        "}";

    private static final String INVALID_PROPERTY_ID_EVENT = "{\n" +
        "  \"version\": \"0\",\n" +
        "  \"id\": \"test-id\",\n" +
        "  \"detail-type\": \"PublicationEvaluationCompleted\",\n" +
        "  \"source\": \"unicorn-approvals\",\n" +
        "  \"account\": \"123456789012\",\n" +
        "  \"time\": \"2022-08-16T06:33:05Z\",\n" +
        "  \"region\": \"us-east-1\",\n" +
        "  \"resources\": [],\n" +
        "  \"detail\": {\n" +
        "    \"property_id\": \"invalid-id\",\n" +
        "    \"evaluation_result\": \"APPROVED\"\n" +
        "  }\n" +
        "}";

    @BeforeEach
    public void setUp() throws Exception {
        handler = new PublicationEvaluationEventHandler();
        Field tableField = PublicationEvaluationEventHandler.class.getDeclaredField("propertyTable");
        tableField.setAccessible(true);
        tableField.set(handler, propertyTable);
    }

    @Test
    public void shouldUpdateStatusToApproved() throws Exception {
        // Given
        Property existingProperty = new Property();
        existingProperty.setCountry("usa");
        existingProperty.setCity("anytown");
        existingProperty.setStreet("main-street");
        existingProperty.setPropertyNumber("123");
        existingProperty.setStatus("PENDING");

        when(propertyTable.getItem(any(Key.class)))
            .thenReturn(CompletableFuture.completedFuture(existingProperty));
        when(propertyTable.putItem(any(Property.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        ByteArrayInputStream inputStream = new ByteArrayInputStream(APPROVED_EVENT.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        handler.handleRequest(inputStream, outputStream, context);

        // Then
        verify(propertyTable).getItem(any(Key.class));
        verify(propertyTable).putItem(any(Property.class));
        String response = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.contains("Successfully updated"));
    }

    @Test
    public void shouldUpdateStatusToDeclined() throws Exception {
        // Given
        Property existingProperty = new Property();
        existingProperty.setCountry("usa");
        existingProperty.setCity("anytown");
        existingProperty.setStreet("main-street");
        existingProperty.setPropertyNumber("123");
        existingProperty.setStatus("PENDING");

        when(propertyTable.getItem(any(Key.class)))
            .thenReturn(CompletableFuture.completedFuture(existingProperty));
        when(propertyTable.putItem(any(Property.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        ByteArrayInputStream inputStream = new ByteArrayInputStream(DECLINED_EVENT.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        handler.handleRequest(inputStream, outputStream, context);

        // Then
        verify(propertyTable).getItem(any(Key.class));
        verify(propertyTable).putItem(any(Property.class));
    }

    @Test
    public void shouldNotUpdateForUnknownResult() {
        // Given
        ByteArrayInputStream inputStream = new ByteArrayInputStream(UNKNOWN_RESULT_EVENT.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When / Then - the handler returns success response since it just skips the update
        assertDoesNotThrow(() -> handler.handleRequest(inputStream, outputStream, context));

        // No DynamoDB operations should occur for unknown evaluation result
        verifyNoInteractions(propertyTable);
    }

    @Test
    public void shouldHandleInvalidPropertyId() {
        // Given - property_id "invalid-id" does not have 4 parts when split by "/"
        ByteArrayInputStream inputStream = new ByteArrayInputStream(INVALID_PROPERTY_ID_EVENT.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When / Then - should throw because property ID format is invalid (not 4 parts)
        assertThrows(RuntimeException.class, () ->
            handler.handleRequest(inputStream, outputStream, context));
    }
}
