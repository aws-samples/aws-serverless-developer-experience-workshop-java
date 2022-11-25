package contracts;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import contracts.utils.Contract;
import contracts.utils.ContractHelper;

import contracts.utils.ContractStatusEnum;

import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;

import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;

/**
 * Handler for requests to Lambda function.
 */
public class UpdateContractFunction
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // Initialise PowerTools
    Logger logger = LogManager.getLogger();

    // Initialise environment variables
    private static String DDB_TABLE = System.getenv("DYNAMODB_TABLE");
    private static String EVENT_BUS = System.getenv("EVENT_BUS");
    private static String SERVICE_NAMESPACE = System.getenv("SERVICE_NAMESPACE");

    ContractHelper helper = new ContractHelper(DDB_TABLE);

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true, correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        // get the payload
        ObjectMapper objectMapper = new ObjectMapper();
        Contract contract;
        try {
            contract = objectMapper.readValue(input.getBody(), Contract.class);
            contract.setContractStatus(ContractStatusEnum.APPROVED);
            if (!validateEvent(contract)) {
                APIGatewayProxyResponseEvent errorResponse = response
                        .withBody("Invalid Body")
                        .withStatusCode(500);

                return errorResponse;
            }
        } catch (JsonProcessingException jsonException) {
            APIGatewayProxyResponseEvent errorResponse = response
                    .withBody("{}")
                    .withStatusCode(500);
            logger.error(jsonException.getLocalizedMessage());
            return errorResponse;
        }

        TracingUtils.putAnnotation("ContractStatus", contract.getContractStatus().toString());

        String existingContract = getExistingContract(contract.getPropertyId());
        if (existingContract == null) {
            APIGatewayProxyResponseEvent errorResponse = response
                    .withBody("{Invalid ContractId}")
                    .withStatusCode(500);
            return errorResponse;
        }
        logger.debug("Contract Found");
        // update the event
        updateContract(contract.getPropertyId());
        logger.debug("Contract Updated");

        try {
            String eventStr = publishEvent(contract.getPropertyId(), existingContract, EVENT_BUS, SERVICE_NAMESPACE,
                    objectMapper);
            logger.debug("Event Sent");
            return response
                    .withStatusCode(200)
                    .withBody(eventStr);

        } catch (JsonProcessingException jsonException) {
            APIGatewayProxyResponseEvent errorResponse = response
                    .withBody("{}")
                    .withStatusCode(500);
            logger.error(jsonException.getLocalizedMessage());
            return errorResponse;
        }

    }

    @Tracing
    private String getExistingContract(String property_id) {

        return helper.propertyExists("property_id", property_id);
    }

    @Tracing
    private void updateContract(String propertyId) {
        helper.updateTableItem(propertyId);
    }

    @Tracing
    private String publishEvent(String propertyId, String contractId, String EVENT_BUS, String SERVICE_NAMESPACE,
            ObjectMapper objectMapper)
            throws JsonProcessingException {
        return helper.sendEvent(propertyId, contractId, ContractStatusEnum.APPROVED, EVENT_BUS, SERVICE_NAMESPACE,
                objectMapper);
    }

    public ContractHelper getHelper() {
        return helper;
    }

    public void setHelper(ContractHelper helper) {
        this.helper = helper;
    }

    private boolean validateEvent(Contract contract) {
        if (contract.getPropertyId() == null) {
            return false;
        } else
            return true;
    }

}
