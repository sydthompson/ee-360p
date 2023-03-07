import java.io.Serializable;

public class Request implements Serializable {
    // Object with optional fields for any type of command
    // Information will be taken based on `operationId` field
    int operationId;
    // Optional fields
    boolean setUdp = false;
    String user;
    String title;
    Integer loanId;

    public Request(int id) {
        this.operationId = id;
    }
}
