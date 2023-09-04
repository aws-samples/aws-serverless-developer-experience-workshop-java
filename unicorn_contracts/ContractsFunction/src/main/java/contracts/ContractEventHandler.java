package contracts;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import contracts.utils.Contract;

import contracts.utils.ContractStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContractEventHandler implements RequestHandler<SQSEvent, Void> {

    // Initialise environment variables
    private static String DDB_TABLE = System.getenv("DYNAMODB_TABLE");
    ObjectMapper objectMapper = new ObjectMapper();

    DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
            .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                    .maxConcurrency(100)
                    .maxPendingConnectionAcquires(10_000))
            .build();

    Logger logger = LogManager.getLogger();
    MetricsLogger metricsLogger = MetricsUtils.metricsLogger();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {

        for (SQSMessage msg : event.getRecords()) {
            // cehck in message attributes about the http method (HttpMethod)
            logger.debug(msg.toString());
            String httpMethod = msg.getMessageAttributes().get("HttpMethod").getStringValue();
            if ("POST".equalsIgnoreCase(httpMethod)) {
                try {
                    createContract(msg.getBody());
                    logger.debug("Contract Saved");
                } catch (JsonProcessingException jsonException) {
                    logger.error("Unknown Exception occoured: " + jsonException.getMessage());
                    logger.fatal(jsonException);
                    jsonException.printStackTrace();
                }

            } else if ("PUT".equalsIgnoreCase(httpMethod)) {
                try {
                    // update the event
                    updateContract(msg.getBody());
                } catch (JsonProcessingException jsonException) {
                    logger.error("Unknown Exception occoured: " + jsonException.getMessage());
                    logger.fatal(jsonException);
                    jsonException.printStackTrace();
                }

            }

        }
        return null;
    }

    @Tracing
    private void createContract(String strContract) throws JsonProcessingException {
        String contractId = UUID.randomUUID().toString();
        Long createDate = new Date().getTime();
        Contract contract = objectMapper.readValue(strContract, Contract.class);

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":cancelled", AttributeValue.builder().s(ContractStatusEnum.CANCELLED.name()).build());
        expressionValues.put(":closed", AttributeValue.builder().s(ContractStatusEnum.CLOSED.name()).build());
        expressionValues.put(":expired", AttributeValue.builder().s(ContractStatusEnum.EXPIRED.name()).build());

        HashMap<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("property_id", AttributeValue.builder().s(contract.getPropertyId()).build());
        itemValues.put("seller_name", AttributeValue.builder().s(contract.getSellerName()).build());
        itemValues.put("contract_created",
                AttributeValue.builder().n(createDate.toString()).build());
        itemValues.put("contract_last_modified_on",
                AttributeValue.builder().n(createDate.toString()).build());
        itemValues.put("contract_id", AttributeValue.builder().s(contractId).build());
        itemValues.put("contract_status", AttributeValue.builder().s(ContractStatusEnum.DRAFT.name()).build());
        itemValues.put("address",
                AttributeValue.builder().s(objectMapper.writeValueAsString(contract.getAddress())).build());
        PutItemRequest putItemRequest = PutItemRequest.builder().tableName(DDB_TABLE)
                .item(itemValues)
                .conditionExpression(
                        "attribute_not_exists(property_id) OR contract_status IN (:cancelled , :closed, :expired)")
                .expressionAttributeValues(expressionValues)
                .build();
        try {
            dynamodbClient.putItem(putItemRequest).join();
        } catch (ConditionalCheckFailedException conditionalCheckFailedException) {
            logger.error("Unable to create contract for Property '" + contract.getPropertyId()
                    + "'.There already is a contract for this property in status " + ContractStatusEnum.DRAFT + " or "
                    + ContractStatusEnum.APPROVED);
        }
    }

    @Tracing
    private void updateContract(String strContract) throws JsonProcessingException {
        Contract contract = objectMapper.readValue(strContract, Contract.class);
        logger.info("Property ID is : " + contract.getPropertyId());
        HashMap<String, AttributeValue> itemKey = new HashMap<>();

        itemKey.put("property_id", AttributeValue.builder().s(contract.getPropertyId()).build());
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":draft", AttributeValue.builder().s(ContractStatusEnum.DRAFT.name()).build());

        HashMap<String, AttributeValueUpdate> updatedValues = new HashMap<>();

        // Update the column specified by name with updatedVal
        updatedValues.put("contract_status", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(ContractStatusEnum.APPROVED.toString()).build())
                .action(AttributeAction.PUT)
                .build());

        updatedValues.put("modified_date", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(String.valueOf(new Date().getTime())).build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(DDB_TABLE)
                .key(itemKey)
                .attributeUpdates(updatedValues)
                .conditionExpression(
                        "attribute_not_exists(property_id) AND contract_status IN (:draft)")
                .expressionAttributeValues(expressionValues)
                .build();
        try {
            dynamodbClient.updateItem(request).join();
        } catch (ConditionalCheckFailedException conditionalCheckFailedException) {
            logger.error("Unable to update contract for Property '" + contract.getPropertyId()
                    + "'.Status is not in " + ContractStatusEnum.DRAFT);
        } catch (ResourceNotFoundException conditionalCheckFailedException) {
            logger.error("Unable to update contract for Property '" + contract.getPropertyId()
                    + "'. Not Found");
        }
    }

    public void setDynamodbClient(DynamoDbAsyncClient dynamodbClient) {
        this.dynamodbClient = dynamodbClient;
    }

}
