package properties;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import properties.dao.ContractStatus;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sfn.SfnAsyncClient;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

public class PropertiesApprovalSyncFunction implements RequestHandler<DynamodbEvent, Serializable> {

    Logger logger = LogManager.getLogger();
    SfnAsyncClient snfClient = SfnAsyncClient.builder()
            .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                    .maxConcurrency(100)
                    .maxPendingConnectionAcquires(10_000))
            .build();

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true)
    public StreamsEventResponse handleRequest(DynamodbEvent input, Context context) {

        List<StreamsEventResponse.BatchItemFailure> batchItemFailures = new ArrayList<>();
        String curRecordSequenceNumber = "";

        for (DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord : input.getRecords()) {
            try {
                // Process your record

                StreamRecord dynamodbRecord = dynamodbStreamRecord.getDynamodb();
                curRecordSequenceNumber = dynamodbRecord.getSequenceNumber();
                Map<String, AttributeValue> newImage = dynamodbRecord.getNewImage();
                Map<String, AttributeValue> oldImage = dynamodbRecord.getOldImage();
                if (oldImage == null) {
                    oldImage = new HashMap<String, AttributeValue>();
                }
                if (newImage == null) {
                    logger.debug("New image is null. Hence return empty stream response");
                    return new StreamsEventResponse();
                }
                // if there is no token do nothing
                if (newImage.get("sfn_wait_approved_task_token") == null
                        && oldImage.get("sfn_wait_approved_task_token") == null) {
                    logger.debug("No task token in both the images. Hence return empty stream response");
                    return new StreamsEventResponse();
                }

                // if contract status is approved, send the task token

                if (!newImage.get("contract_status").getS().equalsIgnoreCase("APPROVED")) {
                    logger.debug("Contract status for property is not APPROVED : " +
                            newImage.get("property_id").getS());
                    return new StreamsEventResponse();
                }
                logger.debug("Contract status for property is APPROVED : " +
                        newImage.get("property_id").getS());

                // send task successful token
                taskSuccessful(newImage.get("sfn_wait_approved_task_token").getS(), newImage);

            } catch (Exception e) {
                /*
                 * Since we are working with streams, we can return the failed item immediately.
                 * Lambda will immediately begin to retry processing from this failed item
                 * onwards.
                 */
                batchItemFailures.add(new StreamsEventResponse.BatchItemFailure(curRecordSequenceNumber));
                return new StreamsEventResponse(batchItemFailures);
            }
        }

        return new StreamsEventResponse();
    }

    private void taskSuccessful(String s, Map<String, AttributeValue> item) throws JsonProcessingException {
        // create the json structure and send the token
        ObjectMapper mapper = new ObjectMapper();
        ContractStatus contractStatus = new ContractStatus();
        contractStatus.setContract_id(item.get("contract_id").getS());
        contractStatus.setContract_status(item.get("contract_status").getS());
        contractStatus.setProperty_id(item.get("property_id").getS());
        contractStatus.setSfn_wait_approved_task_token(item.get("sfn_wait_approved_task_token").getS());
        String taskResult = mapper.writeValueAsString(contractStatus);

        SendTaskSuccessRequest request = SendTaskSuccessRequest.builder()
                .taskToken(contractStatus.getSfn_wait_approved_task_token())
                .output(taskResult)
                .build();
        snfClient.sendTaskSuccess(request).join();

    }
}