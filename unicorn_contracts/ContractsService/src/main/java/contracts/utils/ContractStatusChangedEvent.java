package contracts.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ContractStatusChangedEvent {
    
    @JsonProperty("contract_last_modified_on")
    private Long contractLastModifiedOn;
    
    @JsonProperty("contract_id")
    private String contractId;
    
    @JsonProperty("property_id")
    private String propertyId;
    
    @JsonProperty("contract_status")
    private ContractStatusEnum contractStatus;

    public ContractStatusChangedEvent() {}

    public ContractStatusChangedEvent(String contractId, String propertyId, 
                                    ContractStatusEnum contractStatus, Long contractLastModifiedOn) {
        this.contractId = contractId;
        this.propertyId = propertyId;
        this.contractStatus = contractStatus;
        this.contractLastModifiedOn = contractLastModifiedOn;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractStatusChangedEvent that = (ContractStatusChangedEvent) o;
        return Objects.equals(contractId, that.contractId) &&
               Objects.equals(propertyId, that.propertyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId, propertyId);
    }

    @Override
    public String toString() {
        return "ContractStatusChangedEvent{" +
               "contractId='" + contractId + '\'' +
               ", propertyId='" + propertyId + '\'' +
               ", contractStatus=" + contractStatus +
               ", contractLastModifiedOn=" + contractLastModifiedOn +
               '}';
    }
}
