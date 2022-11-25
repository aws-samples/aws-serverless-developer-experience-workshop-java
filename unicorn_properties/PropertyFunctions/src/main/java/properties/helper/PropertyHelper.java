package properties.helper;

import java.util.HashMap;
import java.util.Map;

import schema.unicorn_contracts.contractstatuschanged.ContractStatusChanged;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class PropertyHelper {

        String tableName;

        DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
                        .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                        .maxConcurrency(100)
                                        .maxPendingConnectionAcquires(10_000))
                        .build();

        public PropertyHelper(String tableName) {
                this.tableName = tableName;
        }

        public void saveContractStatus(
                        ContractStatusChanged contractStatusChanged) {

                Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
                AttributeValue keyvalue = AttributeValue.fromS(contractStatusChanged.getPropertyId());
                key.put("property_id", keyvalue);

                Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
                expressionAttributeValues.put(":t", AttributeValue.fromS(contractStatusChanged.getContractStatus()));
                expressionAttributeValues.put(":c", AttributeValue.fromS(contractStatusChanged.getContractId()));
                expressionAttributeValues.put(":m", AttributeValue
                                .fromN(String.valueOf(contractStatusChanged.getContractLastModifiedOn())));

                UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                                .key(key)
                                .tableName(this.tableName)
                                .updateExpression(
                                                "set contract_status=:t, contract_last_modified_on=:m, contract_id=:c")
                                .expressionAttributeValues(expressionAttributeValues)
                                .build();

                dynamodbClient.updateItem(updateItemRequest).join();

        }

        public Map<String, AttributeValue> getContractStatus(String property_id) {
                HashMap<String, AttributeValue> keyToGet = new HashMap<String, AttributeValue>();
                keyToGet.put("property_id", AttributeValue.builder()
                                .s(property_id).build());

                GetItemRequest request = GetItemRequest.builder()
                                .key(keyToGet)
                                .tableName(this.tableName)
                                .build();

                return dynamodbClient.getItem(request).join().item();

        }

}
