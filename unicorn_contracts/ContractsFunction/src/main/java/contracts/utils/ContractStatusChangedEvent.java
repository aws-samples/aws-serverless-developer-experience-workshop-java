package contracts.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContractStatusChangedEvent {
    @JsonProperty("contract_last_modified_on")
    Long contractLastModifiedOn;
    @JsonProperty("contract_id")
    String contractId;
    @JsonProperty("property_id")
    String propertyId;
    @JsonProperty("contract_status")
    ContractStatusEnum contractStatus;

    public Long getContractLastModifiedOn() {
        return contractLastModifiedOn;
    }

    public void setContractLastModifiedOn(Long contractLastModifiedOn) {
        this.contractLastModifiedOn = contractLastModifiedOn;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public ContractStatusEnum getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(ContractStatusEnum contractStatus) {
        this.contractStatus = contractStatus;
    }

}
