package contracts;

import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import org.junit.Before;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateContractTests {

  Context context;

  ContractEventHandler handler;

  DynamoDbClient client;

  @Before
  public void setUp() throws Exception {

    client = mock(DynamoDbClient.class);
    context = mock(Context.class);

  }

  @ParameterizedTest
  @Event(value = "src/test/events/create_valid_event.json", type = SQSEvent.class)
  public void validEvent(SQSEvent event) {
    DynamoDbClient client = mock(DynamoDbClient.class);
    handler = new ContractEventHandler();
    handler.setDynamodbClient(client);
    handler.handleRequest(event, context);
  }

}
