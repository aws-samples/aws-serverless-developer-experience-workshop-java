package contracts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CreateContractTests {

    @Mock
    private Context context;
    
    @Mock
    private DynamoDbClient dynamoDbClient;
    
    private ContractEventHandler handler;

    @Before
    public void setUp() {
        handler = new ContractEventHandler(dynamoDbClient);
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

    @Test
    public void shouldHandleNullEvent() {
        // When
        handler.handleRequest(null, context);
        
        // Then
        verifyNoInteractions(dynamoDbClient);
    }

    @Test
    public void shouldHandleEmptyEvent() {
        // Given
        SQSEvent emptyEvent = new SQSEvent();
        
        // When
        handler.handleRequest(emptyEvent, context);
        
        // Then
        verifyNoInteractions(dynamoDbClient);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionForMissingHttpMethod() {
        // Given
        SQSEvent event = createTestEventWithoutHttpMethod(
            "{ \"address\": { \"country\": \"USA\", \"city\": \"Anytown\", \"street\": \"Main Street\", \"number\": 123 }, \"seller_name\": \"John Smith\", \"property_id\": \"usa/anytown/main-street/123\"}");
        
        // When
        handler.handleRequest(event, context);
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

    private SQSEvent createTestEventWithoutHttpMethod(String body) {
        SQSEvent event = new SQSEvent();
        SQSMessage message = new SQSMessage();
        message.setMessageId("test-message-id");
        message.setBody(body);
        message.setMessageAttributes(Collections.emptyMap());
        
        event.setRecords(Collections.singletonList(message));
        return event;
    }
}
