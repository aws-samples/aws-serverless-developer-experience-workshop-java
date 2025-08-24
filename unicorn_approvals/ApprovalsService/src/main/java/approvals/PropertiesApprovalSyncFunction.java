package approvals;

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
import approvals.dao.ContractStatus;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sfn.SfnAsyncClient;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lambda function that processes DynamoDB stream events to sync property approval status
 * with Step Functions workflows
 */
public class PropertiesApprovalSyncFunction implements RequestHandler<DynamodbEvent, Serializable> {

    private static final Logger logger = LogManager.getLogger();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String APPROVED_STATUS = "APPROVED";
    
    private final SfnAsyncClient sfnClient = SfnAsyncClient.builder()
        .httpClientBuilder(NettyNioAsyncHttpClient.builder()
            .maxConcurrency(100)
            .maxPendingConnectionAcquires(10_000))
        .build();

    @Tracing
    @Metrics(captureColdStart = true)
    @Logging(logEvent = true)
    public StreamsEventResponse handleRequest(DynamodbEvent input, Context context) {
        List<StreamsEventResponse.BatchItemFailure> batchItemFailures = new ArrayList<>();

        for (DynamodbEvent.DynamodbStreamRecord dynamodbStreamRecord : input.getRecords()) {
            String sequenceNumber = dynamodbStreamRecord.getDynamodb().getSequenceNumber();
            
            try {
                if (!processRecord(dynamodbStreamRecord)) {
                    continue; // Skip this record but don't fail
                }
            } catch (Exception e) {
                logger.error("Failed to process record with sequence number: {}", sequenceNumber, e);
                batchItemFailures.add(new StreamsEventResponse.BatchItemFailure(sequenceNumber));
                return new StreamsEventResponse(batchItemFailures);
            }
        }

        return new StreamsEventResponse();
    }

    private boolean processRecord(DynamodbEvent.DynamodbStreamRecord streamRecord) throws JsonProcessingException {
        StreamRecord dynamodbRecord = streamRecord.getDynamodb();
        Map<String, AttributeValue> newImage = dynamodbRecord.getNewImage();
        Map<String, AttributeValue> oldImage = dynamodbRecord.getOldImage();

        if (newImage == null) {
            logger.debug("New image is null, skipping record");
            return false;
        }

        if (!hasTaskToken(newImage, oldImage)) {
            logger.debug("No task token found in either image, skipping record");
            return false;
        }

        String contractStatus = newImage.get("contract_status").getS();
        String propertyId = newImage.get("property_id").getS();
        
        if (!APPROVED_STATUS.equalsIgnoreCase(contractStatus)) {
            logger.debug("Contract status for property {} is not APPROVED: {}", propertyId, contractStatus);
            return false;
        }

        logger.info("Contract approved for property: {}", propertyId);
        sendTaskSuccess(newImage.get("sfn_wait_approved_task_token").getS(), newImage);
        return true;
    }

    private boolean hasTaskToken(Map<String, AttributeValue> newImage, Map<String, AttributeValue> oldImage) {
        return (newImage.get("sfn_wait_approved_task_token") != null) ||
               (oldImage != null && oldImage.get("sfn_wait_approved_task_token") != null);
    }

    private void sendTaskSuccess(String taskToken, Map<String, AttributeValue> item) throws JsonProcessingException {
        ContractStatus contractStatus = ContractStatus.builder()
            .contractId(item.get("contract_id").getS())
            .contractStatus(item.get("contract_status").getS())
            .propertyId(item.get("property_id").getS())
            .sfnWaitApprovedTaskToken(item.get("sfn_wait_approved_task_token").getS())
            .build();

        String taskResult = objectMapper.writeValueAsString(contractStatus);

        SendTaskSuccessRequest request = SendTaskSuccessRequest.builder()
            .taskToken(taskToken)
            .output(taskResult)
            .build();
            
        sfnClient.sendTaskSuccess(request).join();
        logger.info("Task success sent for property: {}", contractStatus.getPropertyId());
    }
}