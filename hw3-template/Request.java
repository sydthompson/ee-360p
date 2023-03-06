import java.io.Serializable;

public class Request implements Serializable {
    // Object with optional fields for any type of command
    // Information will be taken based on `operationId` field
    int operationId;
    // Optional fields
    String user;
    String title;
    String loanId;

    public Request(int id) {
        this.operationId = id;
    }
}
