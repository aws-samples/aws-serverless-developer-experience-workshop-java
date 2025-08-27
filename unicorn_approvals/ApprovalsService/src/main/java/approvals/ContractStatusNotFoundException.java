package approvals;

public class ContractStatusNotFoundException extends Exception {
    public ContractStatusNotFoundException(String errorMessage) {
        super(errorMessage);
    }
}
