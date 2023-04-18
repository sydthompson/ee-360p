package paxos;

import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: You may need a boolean variable to indicate ack of acceptors and also you may need proposal number and value.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID = 2L;

    public MessageType type;
    public int n_proposal;
    public int n_accept;
    public int n_done;                      // Highest value i passed to done that is known by the process
    public Object value;

    public Response (MessageType type, int n_proposal, int n_accept, int n_done, Object value) {
        this.type = type;
        this.n_proposal = n_proposal;
        this.n_accept = n_accept;
        this.n_done = n_done;
        this.value = value;
    }
    
}
