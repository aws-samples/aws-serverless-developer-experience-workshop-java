package contracts;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;

import org.junit.Before;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import contracts.utils.ContractHelper;

@RunWith(MockitoJUnitRunner.class)
public class UpdateContractTests {

  Context context;

  ContractHelper helper;

  UpdateContractFunction handler;

  @Before
  public void setUp() throws Exception {

    context = mock(Context.class);

  }

  @ParameterizedTest
  @Event(value = "src/test/events/update_valid_event.json", type = APIGatewayProxyRequestEvent.class)
  public void validEvent(APIGatewayProxyRequestEvent event) {
    ContractHelper helper = mock(ContractHelper.class);
    handler = new UpdateContractFunction();
    handler.setHelper(helper);
    Mockito.when(helper.propertyExists(any(), any())).thenReturn("stubbed_Property_id");
    APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);
    assertTrue("Successful Response", response.getStatusCode() == 200);
  }

  @ParameterizedTest
  @Event(value = "src/test/events/update_empty_dict_body_event.json", type = APIGatewayProxyRequestEvent.class)
  public void emptyBody(APIGatewayProxyRequestEvent event) {
    ContractHelper helper = mock(ContractHelper.class);
    handler = new UpdateContractFunction();
    handler.setHelper(helper);
    APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);
    assertTrue("Successful Response", response.getStatusCode() == 500);
  }

  @ParameterizedTest
  @Event(value = "src/test/events/update_missing_body_event.json", type = APIGatewayProxyRequestEvent.class)
  public void missingBody(APIGatewayProxyRequestEvent event) {
    ContractHelper helper = mock(ContractHelper.class);
    handler = new UpdateContractFunction();
    handler.setHelper(helper);
    APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);
    assertTrue("Successful Response", response.getStatusCode() == 500);
  }

  @ParameterizedTest
  @Event(value = "src/test/events/update_wrong_event.json", type = APIGatewayProxyRequestEvent.class)
  public void wrongEvent(APIGatewayProxyRequestEvent event) {
    ContractHelper helper = mock(ContractHelper.class);
    handler = new UpdateContractFunction();
    handler.setHelper(helper);
    APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);
    assertTrue("Successful Response", response.getStatusCode() == 500);
  }

}
