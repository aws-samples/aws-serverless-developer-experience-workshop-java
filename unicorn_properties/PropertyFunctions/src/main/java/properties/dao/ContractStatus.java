package properties.dao;

public class ContractStatus {
    String contract_id;
    String contract_status;
    String property_id;
    String sfn_wait_approved_task_token;

    @Override
    public String toString() {
        return "Property [contract_id=" + contract_id + ", contract_status=" + contract_status + ", property_id="
                + property_id + ", sfn_wait_approved_task_token=" + sfn_wait_approved_task_token + "]";
    }

    public String getContract_id() {
        return contract_id;
    }

    public void setContract_id(String contract_id) {
        this.contract_id = contract_id;
    }

    public String getContract_status() {
        return contract_status;
    }

    public void setContract_status(String contract_status) {
        this.contract_status = contract_status;
    }

    public String getProperty_id() {
        return property_id;
    }

    public void setProperty_id(String property_id) {
        this.property_id = property_id;
    }

    public String getSfn_wait_approved_task_token() {
        return sfn_wait_approved_task_token;
    }

    public void setSfn_wait_approved_task_token(String sfn_wait_approved_task_token) {
        this.sfn_wait_approved_task_token = sfn_wait_approved_task_token;
    }

}
