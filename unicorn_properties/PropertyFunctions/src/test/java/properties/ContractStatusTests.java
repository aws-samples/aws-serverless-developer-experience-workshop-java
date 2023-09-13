package properties;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class ContractStatusTests {

  Context context;
  DynamoDbClient client;

  ContractStatusChangedHandlerFunction contractStatusChangedHandler;
  ContractExistsCheckerFunction contractExistsChecker;

  Map<String, AttributeValue> response = new HashMap<String, AttributeValue>();

  @Before
  public void setUp() throws Exception {

    context = mock(Context.class);
    client = mock(DynamoDbClient.class);

  }

  @Test
  public void validStatusCheckEvent() throws IOException {

    contractStatusChangedHandler = new ContractStatusChangedHandlerFunction();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    File resourceFile = new File("src/test/events/lambda/contract_status_changed.json");
    client = mock(DynamoDbClient.class);
    contractStatusChangedHandler.setDynamodbClient(client);

    FileInputStream fis = new FileInputStream(resourceFile);

    contractStatusChangedHandler.handleRequest(fis, outputStream, context);
    ByteArrayInputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
    String response = new String(inStream.readAllBytes());
    assertTrue("Successful Response", response.contains("contract_id"));

  }

  @Test
  public void validContractCheckEvent() throws IOException, ContractStatusNotFoundException {

    contractExistsChecker = new ContractExistsCheckerFunction();
    client = mock(DynamoDbClient.class);
    contractExistsChecker.setDynamodbClient(client);
    response.put("contract_id", AttributeValue.fromS("value1"));

    Answer<GetItemResponse> answer = new Answer<GetItemResponse>() {
      @Override
      public GetItemResponse answer(InvocationOnMock invocation) throws Throwable {
        return GetItemResponse.builder().item(response).build();
      }

    };
    Mockito.when(client.getItem((GetItemRequest) any()))
        .thenAnswer(answer);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    File resourceFile = new File("src/test/events/lambda/contract_status_checker.json");

    FileInputStream fis = new FileInputStream(resourceFile);

    contractExistsChecker.handleRequest(fis, outputStream, context);
    ByteArrayInputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
    String response = new String(inStream.readAllBytes());
    assertTrue("Successful Response", response.contains("contract_id"));

  }

}
