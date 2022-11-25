package contracts.utils;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ResponseParser {

        Contract parseResponse(Map<String, AttributeValue> queryResponse)
                        throws JsonMappingException, JsonProcessingException {
                Contract response = new Contract();
                ObjectMapper objectMapper = new ObjectMapper();
                Address address = objectMapper.readValue(queryResponse.get("address").s(), Address.class);
                response.setAddress(address);
                response.setContractCreated(
                                Long.valueOf(queryResponse.get("contract_created").s()));
                response.setContractId(queryResponse.get("contract_id").s());
                response.setContractLastModifiedOn(
                                Long.valueOf(queryResponse.get("contract_last_modified_on").s()));
                response.setContractStatus(ContractStatusEnum.valueOf(queryResponse.get("contract_status").s()));
                response.setPropertyId(queryResponse.get("property_id").s());
                response.setSellerName(queryResponse.get("seller_name").s());
                return response;

        }
}
