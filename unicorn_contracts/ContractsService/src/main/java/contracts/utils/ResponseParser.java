package contracts.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;

public class ResponseParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Contract parseResponse(Map<String, AttributeValue> queryResponse) throws JsonProcessingException {
        if (queryResponse == null || queryResponse.isEmpty()) {
            throw new IllegalArgumentException("Query response cannot be null or empty");
        }

        Contract contract = new Contract();
        
        // Parse address
        Optional.ofNullable(queryResponse.get("address"))
                .map(AttributeValue::s)
                .ifPresent(addressJson -> {
                    try {
                        Address address = OBJECT_MAPPER.readValue(addressJson, Address.class);
                        contract.setAddress(address);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to parse address", e);
                    }
                });

        // Parse other fields
        Optional.ofNullable(queryResponse.get("contract_created"))
                .map(AttributeValue::s)
                .ifPresent(contract::setContractCreated);

        Optional.ofNullable(queryResponse.get("contract_id"))
                .map(AttributeValue::s)
                .ifPresent(contract::setContractId);

        Optional.ofNullable(queryResponse.get("contract_last_modified_on"))
                .map(AttributeValue::s)
                .ifPresent(contract::setContractLastModifiedOn);

        Optional.ofNullable(queryResponse.get("contract_status"))
                .map(AttributeValue::s)
                .map(ContractStatusEnum::valueOf)
                .ifPresent(contract::setContractStatus);

        Optional.ofNullable(queryResponse.get("property_id"))
                .map(AttributeValue::s)
                .ifPresent(contract::setPropertyId);

        Optional.ofNullable(queryResponse.get("seller_name"))
                .map(AttributeValue::s)
                .ifPresent(contract::setSellerName);

        return contract;
    }
}
