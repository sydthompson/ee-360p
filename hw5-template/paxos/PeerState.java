package paxos;

public class PeerState {
    int n_a;
    int n_p;
    Object value;
    Object v_a;
    State state;

    public PeerState(int n_a, int n_p, Object value, Object v_a, State state) {
        this.n_a = n_a;
        this.n_p = n_p;
        this.value = value;
        this.v_a = v_a;
        this.state = state;
    }

    public String toString() {
        return String.format("Highest accepted: %d\nHighest value: %s", n_a, v_a.toString());
    }
}