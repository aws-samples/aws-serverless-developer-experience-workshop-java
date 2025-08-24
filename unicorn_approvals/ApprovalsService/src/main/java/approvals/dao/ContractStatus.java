package approvals.dao;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data class representing contract status information
 */
public class ContractStatus {
    
    @JsonProperty("contract_id")
    private final String contractId;
    
    @JsonProperty("contract_status")
    private final String contractStatus;
    
    @JsonProperty("property_id")
    private final String propertyId;
    
    @JsonProperty("sfn_wait_approved_task_token")
    private final String sfnWaitApprovedTaskToken;

    private ContractStatus(Builder builder) {
        this.contractId = builder.contractId;
        this.contractStatus = builder.contractStatus;
        this.propertyId = builder.propertyId;
        this.sfnWaitApprovedTaskToken = builder.sfnWaitApprovedTaskToken;
    }

    public String getContractId() {
        return contractId;
    }

    public String getContractStatus() {
        return contractStatus;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public String getSfnWaitApprovedTaskToken() {
        return sfnWaitApprovedTaskToken;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String contractId;
        private String contractStatus;
        private String propertyId;
        private String sfnWaitApprovedTaskToken;

        public Builder contractId(String contractId) {
            this.contractId = contractId;
            return this;
        }

        public Builder contractStatus(String contractStatus) {
            this.contractStatus = contractStatus;
            return this;
        }

        public Builder propertyId(String propertyId) {
            this.propertyId = propertyId;
            return this;
        }

        public Builder sfnWaitApprovedTaskToken(String sfnWaitApprovedTaskToken) {
            this.sfnWaitApprovedTaskToken = sfnWaitApprovedTaskToken;
            return this;
        }

        public ContractStatus build() {
            return new ContractStatus(this);
        }
    }

    @Override
    public String toString() {
        return "ContractStatus{" +
                "contractId='" + contractId + '\'' +
                ", contractStatus='" + contractStatus + '\'' +
                ", propertyId='" + propertyId + '\'' +
                ", sfnWaitApprovedTaskToken='" + sfnWaitApprovedTaskToken + '\'' +
                '}';
    }
}
