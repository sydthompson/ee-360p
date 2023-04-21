package paxos;

import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the request message for each RMI call.
 * Hint: You may need the sequence number for each paxos instance and also you may need proposal number and value.
 * Hint: Make it more generic such that you can use it for each RMI call.
 * Hint: It is easier to make each variable public
 */
public class Request implements Serializable {
    static final long serialVersionUID = 1L;

    public int seq;
    public int n_clock;
    public Object value;
    public int[] n_done_lowest; //Can be just the peers value

    public Request (int seq, int n_clock, Object value, int[] n_done_lowest) {
        this.seq = seq;
        this.n_clock = n_clock;
        this.value = value;
        this.n_done_lowest = n_done_lowest;
    }
}
