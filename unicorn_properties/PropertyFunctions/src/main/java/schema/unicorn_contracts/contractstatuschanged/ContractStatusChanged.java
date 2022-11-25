package schema.unicorn_contracts.contractstatuschanged;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContractStatusChanged implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonProperty("contract_id")
  private String contractId = null;

  @JsonProperty("contract_last_modified_on")
  private Long contractLastModifiedOn = null;

  @JsonProperty("contract_status")
  private String contractStatus = null;

  @JsonProperty("property_id")
  private String propertyId = null;

  public ContractStatusChanged contractId(String contractId) {
    this.contractId = contractId;
    return this;
  }

  public String getContractId() {
    return contractId;
  }

  public void setContractId(String contractId) {
    this.contractId = contractId;
  }

  public ContractStatusChanged contractLastModifiedOn(Long contractLastModifiedOn) {
    this.contractLastModifiedOn = contractLastModifiedOn;
    return this;
  }

  public Long getContractLastModifiedOn() {
    return contractLastModifiedOn;
  }

  public void setContractLastModifiedOn(Long contractLastModifiedOn) {
    this.contractLastModifiedOn = contractLastModifiedOn;
  }

  public ContractStatusChanged contractStatus(String contractStatus) {
    this.contractStatus = contractStatus;
    return this;
  }

  public String getContractStatus() {
    return contractStatus;
  }

  public void setContractStatus(String contractStatus) {
    this.contractStatus = contractStatus;
  }

  public ContractStatusChanged propertyId(String propertyId) {
    this.propertyId = propertyId;
    return this;
  }

  public String getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(String propertyId) {
    this.propertyId = propertyId;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContractStatusChanged contractStatusChanged = (ContractStatusChanged) o;
    return Objects.equals(this.contractId, contractStatusChanged.contractId) &&
        Objects.equals(this.contractLastModifiedOn, contractStatusChanged.contractLastModifiedOn) &&
        Objects.equals(this.contractStatus, contractStatusChanged.contractStatus) &&
        Objects.equals(this.propertyId, contractStatusChanged.propertyId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(contractId, contractLastModifiedOn, contractStatus, propertyId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContractStatusChanged {\n");

    sb.append("    contractId: ").append(toIndentedString(contractId)).append("\n");
    sb.append("    contractLastModifiedOn: ").append(toIndentedString(contractLastModifiedOn)).append("\n");
    sb.append("    contractStatus: ").append(toIndentedString(contractStatus)).append("\n");
    sb.append("    propertyId: ").append(toIndentedString(propertyId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
