package approvals;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ContractStatusTests {

  Context context;
  DynamoDbClient client;

  ContractStatusChangedHandlerFunction contractStatusChangedHandler;

  Map<String, AttributeValue> response = new HashMap<String, AttributeValue>();

  @Before
  public void setUp() {

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

}
