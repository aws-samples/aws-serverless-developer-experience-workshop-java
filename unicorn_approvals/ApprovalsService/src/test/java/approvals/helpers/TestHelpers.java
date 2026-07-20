package approvals.helpers;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test helper utilities for building DynamoDB Stream and Step Functions events
 * used by the Approvals service tests.
 */
public final class TestHelpers {

    private TestHelpers() {
        // Utility class
    }

    /**
     * Creates a DynamoDB stream event with NewImage and OldImage containing contract status fields.
     *
     * @param propertyId      the property ID
     * @param contractId      the contract ID
     * @param newStatus       the contract status in the NewImage
     * @param oldStatus       the contract status in the OldImage (nullable)
     * @param taskToken       the sfn_wait_approved_task_token value (nullable, placed in NewImage)
     * @param oldTaskToken    the sfn_wait_approved_task_token value for OldImage (nullable)
     * @return a DynamodbEvent with one stream record
     */
    public static DynamodbEvent createDynamoDbStreamEvent(
            String propertyId,
            String contractId,
            String newStatus,
            String oldStatus,
            String taskToken,
            String oldTaskToken) {

        Map<String, AttributeValue> newImage = new HashMap<>();
        newImage.put("property_id", new AttributeValue().withS(propertyId));
        newImage.put("contract_id", new AttributeValue().withS(contractId));
        newImage.put("contract_status", new AttributeValue().withS(newStatus));
        newImage.put("contract_last_modified_on", new AttributeValue().withS("2022-08-25T01:44:02Z"));
        if (taskToken != null) {
            newImage.put("sfn_wait_approved_task_token", new AttributeValue().withS(taskToken));
        }

        Map<String, AttributeValue> oldImage = null;
        if (oldStatus != null) {
            oldImage = new HashMap<>();
            oldImage.put("property_id", new AttributeValue().withS(propertyId));
            oldImage.put("contract_id", new AttributeValue().withS(contractId));
            oldImage.put("contract_status", new AttributeValue().withS(oldStatus));
            oldImage.put("contract_last_modified_on", new AttributeValue().withS("2022-08-24T15:53:26Z"));
            if (oldTaskToken != null) {
                oldImage.put("sfn_wait_approved_task_token", new AttributeValue().withS(oldTaskToken));
            }
        }

        StreamRecord streamRecord = new StreamRecord();
        streamRecord.setNewImage(newImage);
        streamRecord.setOldImage(oldImage);
        streamRecord.setSequenceNumber("123456789");

        DynamodbEvent.DynamodbStreamRecord record = new DynamodbEvent.DynamodbStreamRecord();
        record.setDynamodb(streamRecord);
        record.setEventName("MODIFY");

        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(Collections.singletonList(record));
        return event;
    }

    /**
     * Creates a DynamoDB stream event with a null NewImage (for deletion scenarios).
     *
     * @return a DynamodbEvent with one stream record whose NewImage is null
     */
    public static DynamodbEvent createDynamoDbStreamEventWithNullNewImage() {
        StreamRecord streamRecord = new StreamRecord();
        streamRecord.setNewImage(null);
        streamRecord.setOldImage(null);
        streamRecord.setSequenceNumber("123456789");

        DynamodbEvent.DynamodbStreamRecord record = new DynamodbEvent.DynamodbStreamRecord();
        record.setDynamodb(streamRecord);
        record.setEventName("REMOVE");

        DynamodbEvent event = new DynamodbEvent();
        event.setRecords(Collections.singletonList(record));
        return event;
    }

    /**
     * Creates a Step Functions event JSON string with TaskToken and Input.property_id.
     *
     * @param taskToken  the Step Functions task token
     * @param propertyId the property ID
     * @return a JSON string representing the Step Functions callback event
     */
    public static String createStepFunctionsEvent(String taskToken, String propertyId) {
        return String.format(
            "{\"TaskToken\":\"%s\",\"Input\":{\"property_id\":\"%s\"}}",
            taskToken, propertyId
        );
    }
}
