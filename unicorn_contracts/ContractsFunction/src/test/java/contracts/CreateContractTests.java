package contracts;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import org.junit.Before;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateContractTests {

  Context context;

  ContractEventHandler handler;

  DynamoDbAsyncClient client;

  @Before
  public void setUp() throws Exception {

    client = mock(DynamoDbAsyncClient.class);
    context = mock(Context.class);

  }

  @ParameterizedTest
  @Event(value = "src/test/events/create_valid_event.json", type = SQSEvent.class)
  public void validEvent(SQSEvent event) {
    DynamoDbAsyncClient asyncClient = mock(DynamoDbAsyncClient.class);
    handler = new ContractEventHandler();
    handler.setDynamodbClient(asyncClient);
    handler.handleRequest(event, context);

  }
  /*
   * @ParameterizedTest
   * 
   * @Event(value = "src/test/events/create_empty_dict_body_event.json", type =
   * APIGatewayProxyRequestEvent.class)
   * public void emptyBody(APIGatewayProxyRequestEvent event) {
   * ContractHelper helper = mock(ContractHelper.class);
   * handler = new CreateContractFunction();
   * handler.setHelper(helper);
   * APIGatewayProxyResponseEvent response = handler.handleRequest(event,
   * context);
   * assertTrue("Successful Response", response.getStatusCode() == 500);
   * }
   * 
   * @ParameterizedTest
   * 
   * @Event(value = "src/test/events/create_missing_body_event.json", type =
   * APIGatewayProxyRequestEvent.class)
   * public void missingBody(APIGatewayProxyRequestEvent event) {
   * ContractHelper helper = mock(ContractHelper.class);
   * handler = new CreateContractFunction();
   * handler.setHelper(helper);
   * APIGatewayProxyResponseEvent response = handler.handleRequest(event,
   * context);
   * assertTrue("Successful Response", response.getStatusCode() == 500);
   * }
   * 
   * @ParameterizedTest
   * 
   * @Event(value = "src/test/events/create_wrong_event.json", type =
   * APIGatewayProxyRequestEvent.class)
   * public void wrongEvent(APIGatewayProxyRequestEvent event) {
   * ContractHelper helper = mock(ContractHelper.class);
   * handler = new CreateContractFunction();
   * handler.setHelper(helper);
   * APIGatewayProxyResponseEvent response = handler.handleRequest(event,
   * context);
   * assertTrue("Successful Response", response.getStatusCode() == 500);
   * }
   */

}
