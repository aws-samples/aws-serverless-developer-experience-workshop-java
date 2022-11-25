package property.populate;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;

import property.dao.Property;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.lambda.powertools.cloudformation.AbstractCustomResourceHandler;
import software.amazon.lambda.powertools.cloudformation.Response;

public class PopulateDataFunction extends AbstractCustomResourceHandler {

    String DYNAMODB_TABLE = System.getenv("DYNAMODB_TABLE");

    String[] validKeys = { "country", "city", "street", "number", "description", "contract", "listprice", "currency",
            "images" };

    DynamoDbAsyncClient dynamodbClient = DynamoDbAsyncClient.builder()
            .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                    .maxConcurrency(100)
                    .maxPendingConnectionAcquires(10_000))
            .build();

    DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(dynamodbClient)
            .build();

    @Override
    protected Response create(CloudFormationCustomResourceEvent createEvent, Context context) {
        try {
            return handleEvent(createEvent);
        } catch (Exception e) {
            return Response.builder()
                    .value(Map.of("Resource", DYNAMODB_TABLE + "-" + createEvent.getLogicalResourceId()))
                    .status(Response.Status.FAILED).build();
        }

    }

    @Override
    protected Response update(CloudFormationCustomResourceEvent updateEvent, Context context) {
        try {
            return handleEvent(updateEvent);
        } catch (Exception e) {
            return Response.builder()
                    .value(Map.of("Resource", DYNAMODB_TABLE + "-" + updateEvent.getLogicalResourceId()))
                    .status(Response.Status.FAILED).build();
        }
    }

    @Override
    protected Response delete(CloudFormationCustomResourceEvent deleteEvent, Context context) {
        return null;
    }

    private void saveInDatabase(Property property, String table_name) {

        DynamoDbAsyncTable<Property> propertyTable = enhancedClient.table(table_name,
                TableSchema.fromBean(Property.class));
        propertyTable.putItem(property).join();

    }

    Property createPropertyFromEvent(CloudFormationCustomResourceEvent event) throws Exception {
        Map<String, Object> propertyMap = event.getResourceProperties();
        // Iterate over map and check the keys
        for (String strKey : validKeys) {

            if (!propertyMap.containsKey(strKey)) {
                throw new Exception("Invalid input: missing mandatory field " + strKey);
            }

        }

        Property property = new Property();
        property.setCountry((String) propertyMap.get("country"));
        property.setCity((String) propertyMap.get("city"));
        property.setStreet((String) propertyMap.get("street"));
        property.setPropertyNumber((String) propertyMap.get("number"));
        property.setDescription((String) propertyMap.get("description"));
        property.setContract((String) propertyMap.get("contract"));
        property.setListprice(Float.parseFloat((String) propertyMap.get("listprice")));
        property.setCurrency((String) propertyMap.get("currency"));
        List<String> images = (List<String>) propertyMap.get("images");
        property.setImages(images);
        property.setStatus("NEW");
        return property;

    }

    private Response handleEvent(CloudFormationCustomResourceEvent createEvent) throws Exception {

        Property property = createPropertyFromEvent(createEvent);
        saveInDatabase(property, DYNAMODB_TABLE);
        return Response.builder().value(Map.of("Resource", DYNAMODB_TABLE + "-" + createEvent.getLogicalResourceId()))
                .status(Response.Status.SUCCESS).build();
    }
}
