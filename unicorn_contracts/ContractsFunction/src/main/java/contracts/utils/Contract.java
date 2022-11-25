package contracts.utils;

import com.fasterxml.jackson.annotation.JsonAlias;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class Contract {

    Address address;
    @JsonAlias("property_id")
    String propertyId;
    @JsonAlias("contract_id")
    String contractId;
    @JsonAlias("seller_name")
    String sellerName;
    @JsonAlias("contract_status")
    ContractStatusEnum contractStatus;
    @JsonAlias("contract_created")
    Long contractCreated;
    @JsonAlias("contract_last_modified_on")
    Long contractLastModifiedOn;

    @DynamoDbAttribute("address")
    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @DynamoDbPartitionKey
    @DynamoDbAttribute("property_id")
    public String getPropertyId() {
        return this.propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    @DynamoDbAttribute("contract_id")
    public String getContractId() {
        return this.contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    @DynamoDbAttribute("seller_name")
    public String getSellerName() {
        return this.sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    @DynamoDbAttribute("contract_status")
    public ContractStatusEnum getContractStatus() {
        return this.contractStatus;
    }

    public void setContractStatus(ContractStatusEnum contractStatus) {
        this.contractStatus = contractStatus;
    }

    @DynamoDbAttribute("contract_created")
    public Long getContractCreated() {
        return this.contractCreated;
    }

    public void setContractCreated(Long contractCreated) {
        this.contractCreated = contractCreated;
    }

    @DynamoDbAttribute("contract_last_modified_on")
    public Long getContractLastModifiedOn() {
        return this.contractLastModifiedOn;
    }

    public void setContractLastModifiedOn(Long contractLastModifiedOn) {
        this.contractLastModifiedOn = contractLastModifiedOn;
    }

}
