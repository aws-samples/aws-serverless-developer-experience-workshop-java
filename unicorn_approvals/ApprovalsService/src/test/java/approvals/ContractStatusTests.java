package approvals;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ContractStatusTests {

    @Mock
    private Context context;

    @Mock
    private DynamoDbClient dynamoDbClient;

    private ContractStatusChangedHandlerFunction contractStatusChangedHandler;

    @BeforeEach
    public void setUp() {
        contractStatusChangedHandler = new ContractStatusChangedHandlerFunction();
        contractStatusChangedHandler.setDynamodbClient(dynamoDbClient);
    }

    @Test
    public void shouldProcessValidContractStatusChangeEvent() throws IOException {
        // Given
        Path testEventPath = Paths.get("src/test/events/lambda/contract_status_changed.json");

        // When
        try (InputStream inputStream = Files.newInputStream(testEventPath);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            contractStatusChangedHandler.handleRequest(inputStream, outputStream, context);

            // Then
            String response = outputStream.toString();
            assertTrue(response.contains("contract_id"));
        }
    }

    @Test
    public void shouldHandleMalformedEvent() {
        // Given
        String malformedJson = "{ not valid json }";
        InputStream inputStream = new ByteArrayInputStream(malformedJson.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When / Then
        assertThrows(Exception.class, () ->
            contractStatusChangedHandler.handleRequest(inputStream, outputStream, context));
    }

    @Test
    public void shouldHandleDynamoDbFailure() throws IOException {
        // Given
        Path testEventPath = Paths.get("src/test/events/lambda/contract_status_changed.json");
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("DynamoDB failure").build());

        // When / Then
        try (InputStream inputStream = Files.newInputStream(testEventPath);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            assertThrows(DynamoDbException.class, () ->
                contractStatusChangedHandler.handleRequest(inputStream, outputStream, context));
        }
    }
}
