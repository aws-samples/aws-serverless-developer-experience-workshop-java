package contracts.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;

import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.utils.ImmutableMap;

public class ContractHelper {
        static String ContractCreatedMetric = "ContractCreated";
        static String ContractUpdatedMetric = "ContractUpdated";
        static String ContractEventMetric = "ContractEvent";

        String tableName;

        public ContractHelper(DynamoDbAsyncClient dynamodbClient, EventBridgeAsyncClient eventBridgeClient) {
                this.dynamodbClient = dynamodbClient;
                this.eventBridgeClient = eventBridgeClient;
        }

        public ContractHelper() {

        }

        public ContractHelper(String tableName) {
                this.tableName = tableName;

        }

        DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
                        .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                        .maxConcurrency(100)
                                        .maxPendingConnectionAcquires(10_000))
                        .build();

        EventBridgeAsyncClient eventBridgeClient = EventBridgeAsyncClient.builder()
                        .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                        .build();

        DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                        .dynamoDbClient(dynamodbClient)
                        .build();

        private List<Map<String, AttributeValue>> queryDatabase(String TABLE_NAME,
                        HashMap<String, AttributeValue> attrValues) {

                QueryRequest queryReq = QueryRequest.builder()
                                .tableName(TABLE_NAME)
                                .keyConditionExpression("property_id =:v_pk")
                                .expressionAttributeValues(attrValues)
                                .build();

                return dynamodbClient.query(queryReq).join().items();

        }

        public String propertyExists(String key, String keyVal) {

                // Set up mapping of the partition name with the value.
                HashMap<String, AttributeValue> attrValues = new HashMap<>();

                attrValues.put(":v_pk", AttributeValue.builder().s(keyVal).build());

                List<Map<String, AttributeValue>> ddbResponse = queryDatabase(this.tableName, attrValues);
                if (ddbResponse.size() >= 1)
                        return ddbResponse.get(0).get(
                                        "contract_id").s();
                else
                        return null;

        }

        public void updateTableItem(
                        String keyVal) {

                HashMap<String, AttributeValue> itemKey = new HashMap<>();

                itemKey.put("property_id", AttributeValue.builder().s(keyVal).build());

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
                                .tableName(this.tableName)
                                .key(itemKey)
                                .attributeUpdates(updatedValues)
                                .build();

                dynamodbClient.updateItem(request);

        }

        public String sendEvent(String propertyId, String contractId, ContractStatusEnum status, String EVENT_BUS,
                        String SERVICE_NAMESPACE,
                        ObjectMapper objectMapper)
                        throws JsonProcessingException {
                ContractStatusChangedEvent event = new ContractStatusChangedEvent();
                event.setContractId(contractId);
                event.setContractLastModifiedOn(new Date().getTime());
                event.setContractStatus(status);
                event.setPropertyId(propertyId);

                String event_string = objectMapper.writeValueAsString(event);

                List<PutEventsRequestEntry> requestEntries = new ArrayList<>();

                requestEntries.add(PutEventsRequestEntry.builder()
                                .eventBusName(EVENT_BUS)
                                .source(SERVICE_NAMESPACE)
                                .detailType("ContractStatusChanged")
                                .detail(event_string).build());
                PutEventsRequest eventsRequest = PutEventsRequest.builder().entries(requestEntries).build();

                eventBridgeClient.putEvents(eventsRequest).join();

                return event_string;
        }

        public void createContract(Contract contract) {

                DynamoDbAsyncTable<Contract> contractTable = enhancedClient.table(this.tableName,
                        TableSchema.fromBean(Contract.class));
                contractTable.putItem(contract).join();

        }

        public DynamoDbAsyncClient getDynamodbClient() {
                return dynamodbClient;
        }

        public void setDynamodbClient(DynamoDbAsyncClient dynamodbClient) {
                this.dynamodbClient = dynamodbClient;
        }

        public EventBridgeAsyncClient getEventBridgeClient() {
                return eventBridgeClient;
        }

        public void setEventBridgeClient(EventBridgeAsyncClient eventBridgeClient) {
                this.eventBridgeClient = eventBridgeClient;
        }
}
