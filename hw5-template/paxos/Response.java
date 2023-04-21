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
    public int n_a;
    public Object v_a;
    public int[] n_done_lowest;

    public Response (MessageType type, int n_a, Object v_a, int[] n_done_lowest) {
        this.type = type;
        this.n_a = n_a;
        this.v_a = v_a;
        this.n_done_lowest = n_done_lowest;
    }
    
    public String toString() {
        return String.format("%s, %d", type.toString(), n_a);
    }
}
