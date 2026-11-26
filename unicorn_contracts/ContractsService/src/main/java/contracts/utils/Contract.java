package contracts.utils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Contract {

    @JsonProperty("address")
    private Address address;
    
    @JsonProperty("property_id")
    @JsonAlias("property_id")
    private String propertyId;
    
    @JsonProperty("contract_id")
    @JsonAlias("contract_id")
    private String contractId;
    
    @JsonProperty("seller_name")
    @JsonAlias("seller_name")
    private String sellerName;
    
    @JsonProperty("contract_status")
    @JsonAlias("contract_status")
    private ContractStatusEnum contractStatus;
    
    @JsonProperty("contract_created")
    @JsonAlias("contract_created")
    private String contractCreated;

    @JsonProperty("contract_last_modified_on")
    @JsonAlias("contract_last_modified_on")
    private String contractLastModifiedOn;

    public Contract() {}

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public ContractStatusEnum getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(ContractStatusEnum contractStatus) {
        this.contractStatus = contractStatus;
    }

    public String getContractCreated() {
        return contractCreated;
    }

    public void setContractCreated(String contractCreated) {
        this.contractCreated = contractCreated;
    }

    public String getContractLastModifiedOn() {
        return contractLastModifiedOn;
    }

    public void setContractLastModifiedOn(String contractLastModifiedOn) {
        this.contractLastModifiedOn = contractLastModifiedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contract contract = (Contract) o;
        return Objects.equals(propertyId, contract.propertyId) &&
               Objects.equals(contractId, contract.contractId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyId, contractId);
    }

    @Override
    public String toString() {
        return "Contract{" +
               "propertyId='" + propertyId + '\'' +
               ", contractId='" + contractId + '\'' +
               ", sellerName='" + sellerName + '\'' +
               ", contractStatus=" + contractStatus +
               '}';
    }
}
