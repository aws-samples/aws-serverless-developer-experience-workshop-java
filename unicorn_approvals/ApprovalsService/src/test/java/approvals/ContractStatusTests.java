package approvals;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ContractStatusTests {

    @Mock
    private Context context;
    
    @Mock
    private DynamoDbClient dynamoDbClient;

    private ContractStatusChangedHandlerFunction contractStatusChangedHandler;

    @Before
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
            assertTrue("Response should contain contract_id", response.contains("contract_id"));
        }
    }
}
