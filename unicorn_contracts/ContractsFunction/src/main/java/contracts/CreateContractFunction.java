package contracts;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.lambda.powertools.logging.CorrelationIdPathConstants;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;

/**
 * Handler for requests to Lambda function.
 */
public class CreateContractFunction
                implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

        // Initialise PowerTools
        Logger logger = LogManager.getLogger();
        MetricsLogger metricsLogger = MetricsUtils.metricsLogger();

        // Initialise environment variables
        private static String DDB_TABLE = System.getenv("DYNAMODB_TABLE");
        private static String EVENT_BUS = System.getenv("EVENT_BUS");
        private static String SERVICE_NAMESPACE = System.getenv("SERVICE_NAMESPACE");

        ContractHelper helper = new ContractHelper(DDB_TABLE);

        @Tracing
        @Metrics(captureColdStart = true)
        @Logging(logEvent = true, correlationIdPath = CorrelationIdPathConstants.API_GATEWAY_REST)
        public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input,
                        final Context context) {

                String contractId = UUID.randomUUID().toString();
                Long createDate = new Date().getTime();

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

                        if (!validateEvent(contract)) {
                                APIGatewayProxyResponseEvent errorResponse = response
                                                .withBody("Invalid Body")
                                                .withStatusCode(500);

                                return errorResponse;
                        }
                        contract.setContractId(contractId);
                        contract.setContractLastModifiedOn(createDate);
                        contract.setContractCreated(createDate);
                        contract.setContractStatus(ContractStatusEnum.DRAFT);
                } catch (JsonProcessingException jsonException) {
                        APIGatewayProxyResponseEvent errorResponse = response
                                        .withBody(jsonException.getLocalizedMessage())
                                        .withStatusCode(500);
                        logger.error(jsonException.getLocalizedMessage());
                        return errorResponse;
                }
                logger.debug("Input parsing completed");
                // write to dynamodb table
                try {
                        createContract(contract);
                        logger.debug("Contract Saved");
                        TracingUtils.putAnnotation("ContractStatus", contract.getContractStatus().toString());

                        // send the event

                        String eventStr = publishEvent(contract, EVENT_BUS, SERVICE_NAMESPACE, objectMapper);
                        logger.debug("Event Sent");
                        return response
                                        .withStatusCode(200)
                                        .withBody(eventStr);

                } catch (JsonProcessingException jsonException) {
                        APIGatewayProxyResponseEvent errorResponse = response
                                        .withBody(jsonException.getLocalizedMessage())
                                        .withStatusCode(500);
                        logger.error(jsonException.getLocalizedMessage());
                        return errorResponse;
                }

        }

        @Tracing
        private String publishEvent(Contract contract, String EVENT_BUS, String SERVICE_NAMESPACE,
                        ObjectMapper objectMapper)
                        throws JsonProcessingException {
                return helper.sendEvent(contract.getPropertyId(), contract.getContractId(), ContractStatusEnum.DRAFT,
                                EVENT_BUS,
                                SERVICE_NAMESPACE, objectMapper);
        }

        @Tracing
        private void createContract(Contract contract) throws JsonProcessingException {
                DynamoDbAsyncTable<Contract> contractTable = enhancedClient.table(this.tableName,
                                TableSchema.fromBean(Contract.class));
                contractTable.putItem(contract).join();
        }

        public ContractHelper getHelper() {
                return helper;
        }

        public void setHelper(ContractHelper helper) {
                this.helper = helper;
        }

        public boolean validateEvent(Contract contract) {
                if (contract.getAddress() == null || contract.getPropertyId() == null
                                || contract.getSellerName() == null) {
                        return false;
                } else
                        return true;
        }

}
