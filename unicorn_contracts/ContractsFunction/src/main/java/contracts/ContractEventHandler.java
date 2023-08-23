package contracts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.utils.Contract;
import contracts.utils.ContractHelper;
import contracts.utils.ContractStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;

import java.util.Date;
import java.util.UUID;

public class ContractEventHandler implements RequestHandler<SQSEvent, Void>{

    // Initialise environment variables
    private static String DDB_TABLE = System.getenv("DYNAMODB_TABLE");
    private static String SERVICE_NAMESPACE = System.getenv("SERVICE_NAMESPACE");

    ContractHelper helper = new ContractHelper(DDB_TABLE);

    Logger logger = LogManager.getLogger();
    MetricsLogger metricsLogger = MetricsUtils.metricsLogger();

    @Override
    public Void handleRequest(SQSEvent event, Context context)
    {
        ObjectMapper objectMapper = new ObjectMapper();

        for(SQSMessage msg : event.getRecords()){
            //cehck in message attributes about the http method (HttpMethod)
            logger.debug(msg.toString());
            String httpMethod  = msg.getMessageAttributes().get("HttpMethod").getStringValue();
            if("POST".equalsIgnoreCase(httpMethod))
            {
                //create a dynamodb object
                String contractId = UUID.randomUUID().toString();
                Long createDate = new Date().getTime();
                Contract contract;

                try {
                    contract = objectMapper.readValue(msg.getBody(), Contract.class);
                    contract.setContractId(contractId);
                    contract.setContractLastModifiedOn(createDate);
                    contract.setContractCreated(createDate);
                    contract.setContractStatus(ContractStatusEnum.DRAFT);
                    createContract(contract);
                    logger.debug("Contract Saved");
                }
                catch (JsonProcessingException jsonException) {
                    logger.error("Unknown Exception occoured: "+jsonException.getMessage());
                    logger.fatal(jsonException);
                    jsonException.printStackTrace();

                }



            }
            else if("PUT".equalsIgnoreCase(httpMethod)){
                Contract contract;
                try {
                    contract = objectMapper.readValue(msg.getBody(), Contract.class);
                    logger.info("Property ID is : "+contract.getPropertyId());
                    // update the event
                    updateContract(contract.getPropertyId());
                    logger.info("Update Complete : "+contract.getPropertyId());

                } catch (JsonProcessingException jsonException) {
                    logger.error("Unknown Exception occoured: "+jsonException.getMessage());
                    logger.fatal(jsonException);
                    jsonException.printStackTrace();
                }

            }



        }
        return null;
    }
    @Tracing
    private void createContract(Contract contract) throws JsonProcessingException {
        helper.createContract(contract);
    }



    @Tracing
    private void updateContract(String propertyId) {
        helper.updateTableItem(propertyId);
    }

}
