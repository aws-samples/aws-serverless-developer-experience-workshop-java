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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import properties.helper.PropertyHelper;

@RunWith(MockitoJUnitRunner.class)
public class ContractStatusTests {

  Context context;

  ContractStatusChangedHandlerFunction contractStatusChangedHandler;
  ContractExistsCheckerFunction contractExistsChecker;

  PropertyHelper helper;

  Map<String, AttributeValue> response = new HashMap<String, AttributeValue>();

  @Before
  public void setUp() throws Exception {

    context = mock(Context.class);

  }

  @Test
  public void validStatusCheckEvent() throws IOException {

    contractStatusChangedHandler = new ContractStatusChangedHandlerFunction();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    File resourceFile = new File("src/test/events/lambda/contract_status_changed.json");
    helper = mock(PropertyHelper.class);
    contractStatusChangedHandler.setHelper(helper);

    FileInputStream fis = new FileInputStream(resourceFile);

    contractStatusChangedHandler.handleRequest(fis, outputStream, context);
    ByteArrayInputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
    String response = new String(inStream.readAllBytes());
    assertTrue("Successful Response", response.contains("contract_id"));

  }

  @Test
  public void validContractCheckEvent() throws IOException, ContractStatusNotFoundException {

    contractExistsChecker = new ContractExistsCheckerFunction();
    helper = mock(PropertyHelper.class);
    response.put("contract_id", AttributeValue.fromS("value1"));
    Mockito.when(helper.getContractStatus(any())).thenReturn(response);
    contractExistsChecker.setHelper(helper);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    File resourceFile = new File("src/test/events/lambda/contract_status_checker.json");

    FileInputStream fis = new FileInputStream(resourceFile);

    contractExistsChecker.handleRequest(fis, outputStream, context);
    ByteArrayInputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
    String response = new String(inStream.readAllBytes());
    assertTrue("Successful Response", response.contains("contract_id"));

  }

}
