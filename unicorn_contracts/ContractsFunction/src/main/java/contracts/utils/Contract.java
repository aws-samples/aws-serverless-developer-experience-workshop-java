package contracts.utils;

import com.fasterxml.jackson.annotation.JsonAlias;

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

    public Address getAddress() {
        return this.address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getPropertyId() {
        return this.propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getContractId() {
        return this.contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getSellerName() {
        return this.sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public ContractStatusEnum getContractStatus() {
        return this.contractStatus;
    }

    public void setContractStatus(ContractStatusEnum contractStatus) {
        this.contractStatus = contractStatus;
    }

    public Long getContractCreated() {
        return this.contractCreated;
    }

    public void setContractCreated(Long contractCreated) {
        this.contractCreated = contractCreated;
    }

    public Long getContractLastModifiedOn() {
        return this.contractLastModifiedOn;
    }

    public void setContractLastModifiedOn(Long contractLastModifiedOn) {
        this.contractLastModifiedOn = contractLastModifiedOn;
    }

}
