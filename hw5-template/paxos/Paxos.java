package paxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;

/**
 * This class is the main class you need to implement paxos instances.
 * It corresponds to a single Paxos peer.
 */
public class Paxos implements PaxosRMI, Runnable {

    ReentrantLock mutex;
    String[] peers;             // hostnames of all peers
    int[] ports;                // ports of all peers
    int me;                     // this peer's index into peers[] and ports[]

    Registry registry;
    PaxosRMI stub;

    // v corresponds to the value to be proposed by this paxos instance
    int seq;
    Object value;

    AtomicBoolean dead,
                  unreliable;   // for testing

    int[] n_done_lowest;

    ConcurrentHashMap<Integer, PeerState> v_decided = new ConcurrentHashMap<Integer, PeerState>();
    Semaphore semaphore = new Semaphore(1);
    ReentrantLock done_lowest_lock;

    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Paxos(int me, String[] peers, int[] ports) {
        this.me = me;
        this.peers = peers;
        this.ports = ports;

        this.mutex = new ReentrantLock();
        this.dead = new AtomicBoolean(false);
        this.unreliable = new AtomicBoolean(false);
        this.done_lowest_lock = new ReentrantLock();

        n_done_lowest = new int[peers.length];
        for(int i = 0; i < n_done_lowest.length; i++) { n_done_lowest[i]=-1; }
        // register peers, do not modify this part

        try {
            System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
            registry = LocateRegistry.createRegistry(this.ports[this.me]);
            stub = (PaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("Paxos", stub);
        } catch (Exception e) {
           // e.printStackTrace();
        }
    }

    /**
     * Call() sends an RMI to the RMI handler on server with
     * arguments rmi name, request message, and server id. It
     * waits for the reply and return a response message if
     * the server responded, and return null if Call() was not
     * be able to contact the server.
     *
     * You should assume that Call() will time out and return
     * null after a while if it doesn't get a reply from the server.
     *
     * Please use Call() to send all RMIs and please don't change
     * this function.
     */
    public Response Call(String rmi, Request req, int id) {
        Response callReply = null;

        PaxosRMI stub;
        try {
            Registry registry = LocateRegistry.getRegistry(this.ports[id]);
            stub = (PaxosRMI) registry.lookup("Paxos");
            if (rmi.equals("Prepare"))
                callReply = stub.Prepare(req);
            else if (rmi.equals("Accept"))
                callReply = stub.Accept(req);
            else if (rmi.equals("Decide"))
                callReply = stub.Decide(req);
            else
                System.out.println("Wrong parameters!");
        } catch (Exception e) {
            return null;
        }
        return callReply;
    }

    /**
     * The application wants Paxos to start agreement on instance seq,
     * with proposed value v. Start() should start a new thread to run
     * Paxos on instance seq. Multiple instances can be run concurrently.
     *
     * Hint: You may start a thread using the runnable interface of
     * Paxos object. One Paxos object may have multiple instances, each
     * instance corresponds to one proposed value/command. Java does not
     * support passing arguments to a thread, so you may reset seq and v
     * in Paxos object before starting a new thread. There is one issue
     * that variable may change before the new thread actually reads it.
     * Test won't fail in this case.
     *
     * Start() just starts a new thread to initialize the agreement.
     * The application will call Status() to find out if/when agreement
     * is reached.
     */
    public void Start(int seq, Object value) {
        // Start the PAXOS instance, add it within the static var for instances

        //ignore Start() call if seq is less than min
        //if(seq < Min()) return;
        try {
            semaphore.acquire();
        } catch(Exception e) {
        }
        this.seq = seq;
        this.value = value;
        new Thread(this).start();
    }
    
    /*
     * PROPOSER
     */
    @Override
    public void run() {
        // Peer versions
        int p_seq = seq;
        int n_clock = me;
        Object p_value = value;
        State p_state = State.Pending;

        int highest_n = -1;
        Object highest_v = null;
        semaphore.release();

        while (p_state == State.Pending) {
            n_clock += peers.length;

            if(isDead()) return;

            int num_prepare = 0, 
                num_accept = 0;

            Request request = new Request(p_seq, n_clock, p_value, n_done_lowest);

            // Send to all instances, check if receive ok from more than half of array length
            for (int i = 0; i < peers.length; i++) {
                
                Response r_prepare;
                if (i == me) r_prepare = Prepare(request);
                else r_prepare = Call("Prepare", request, i);

                
                if (r_prepare != null) {
                    if (r_prepare.type == MessageType.PREPARE_OK) {
                        num_prepare++;
                        if (r_prepare.n_a > highest_n) {
                            highest_n = r_prepare.n_a;
                            highest_v = r_prepare.v_a;
                        }
                    }
                    // Now check if n done needs to be replaced
                    updateDoneLowest(r_prepare);
                } 
            }
            if (seq == 0) System.out.println(String.format("Theirs: %d, %s, Mine: %d", highest_n, highest_v, n_clock));

            // Check for majority OK, then move to accept
            if (num_prepare >= (peers.length + 1) / 2) {
                //If highest_n != -1 (invalid) -> Someone sent me a value to commit to
                // Choose highest_v, else choose value
                Object send_value;

                if (highest_n != -1) send_value = highest_v;
                else send_value = p_value;
                Request a_request = new Request(p_seq, n_clock, send_value, n_done_lowest);
                
                for (int i = 0; i < peers.length; i++) {

                    Response r_accept;
                    if (i == me) r_accept = Accept(a_request);
                    else r_accept = Call("Accept", a_request, i);

                    if (r_accept != null) {
                        if (r_accept.type == MessageType.ACCEPT_OK) num_accept++;
                        updateDoneLowest(r_accept);
                    }

                }

                // Majority OK, move to decide
                if (num_accept >= (peers.length + 1)/ 2) {
                    p_state = State.Decided;
                    for (int i = 0; i < peers.length; i++) {
                    // Decide request in case n and v have changed
                        Request d_request = new Request(p_seq, n_clock, send_value, n_done_lowest);
                        if (i == me) Decide(d_request);
                        else Call("Decide", d_request, i);
                        // Do something with min done value here
                    }
                }
                //System.out.println(String.format("We decided on: %s", v_decided.get(p_seq).value));
            } 
        }
    }
    
    private void updateDoneLowest(Request req) {
        done_lowest_lock.lock();
        for(int a=0; a < n_done_lowest.length; a++) {
            if(n_done_lowest[a] < req.n_done_lowest[a]) {
                n_done_lowest[a] = req.n_done_lowest[a];
            }
        } done_lowest_lock.unlock();

    }

    private void updateDoneLowest(Response res) {
        done_lowest_lock.lock();
        for(int a=0; a < n_done_lowest.length; a++) {
            if(n_done_lowest[a] < res.n_done_lowest[a]) {
                n_done_lowest[a] = res.n_done_lowest[a];
            }
        } 
        done_lowest_lock.unlock();

    }


    /*
     * ACCEPTOR
     */

    // RMI Handler for prepare requests
    public Response Prepare(Request req) {

        if (v_decided.get(req.seq) == null) {
            v_decided.put(
                req.seq,
                new PeerState(-1, -1, null, null, State.Pending)
            );
        }

        updateDoneLowest(req);

        Response r;
        if (req.n_clock > v_decided.get(req.seq).n_p) {
            // PREPARE OK
            v_decided.get(req.seq).n_p = req.n_clock;
            r = new Response(
                MessageType.PREPARE_OK,
                v_decided.get(req.seq).n_a,
                v_decided.get(req.seq).v_a,
                n_done_lowest);
        } else {
            // PREPARE REJECT
            r = new Response(
                MessageType.PREPARE_REJECT, 
                v_decided.get(req.seq).n_a,
                v_decided.get(req.seq).v_a,
                n_done_lowest);
        }
        return r;
    }

    // RMI Handler for accept requests
    public Response Accept(Request req) {
        updateDoneLowest(req);
 
        Response r;

        if (req.n_clock >= v_decided.get(req.seq).n_p) {
            v_decided.get(req.seq).n_p = req.n_clock;      // n_p = n
            v_decided.get(req.seq).n_a = req.n_clock;      // n_a = n
            v_decided.get(req.seq).v_a = req.value;        // v_a = v
            
            // ACCEPT OK
            r = new Response(
                MessageType.ACCEPT_OK, 
                v_decided.get(req.seq).n_a,
                v_decided.get(req.seq).v_a,
                n_done_lowest);
        } else {
            // ACCEPT REJECT
            r = new Response(
                MessageType.ACCEPT_REJECT,
                v_decided.get(req.seq).n_a,
                v_decided.get(req.seq).v_a,
                n_done_lowest);
        }
        return r;
    }

    /*
     * WHEN DECISION REACHED
     */

    // RMI Handler for decide requests
    public Response Decide(Request req) {
        //updateDoneLowest(req);
        v_decided.get(req.seq).state = State.Decided;
        v_decided.get(req.seq).value = req.value;

        return new Response(
            MessageType.DECIDE_OK,
            v_decided.get(req.seq).n_a,
            v_decided.get(req.seq).value,
            n_done_lowest
        );
    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
        // System.out.println("Entering Done w seq: " + seq);
        // System.out.println("original state: " + help(n_done_lowest));
        int min_sequence = Min();
        n_done_lowest[me]=seq;
        // find out which 
        if(this.seq < min_sequence) {
            PeerState ps = v_decided.get(seq);
            v_decided.get(seq).state = State.Forgotten;
        }
        //TODO send the highest Done argument supplied by local application
        // System.out.println("After done: " + help(n_done_lowest));
    }

    private String help(int[] arr) {
        String s = "[";
        for(int i : arr) {s += i + ", ";}
        s= s.substring(0, s.length() - 2);
        s+= "]";
        return s;
    }

    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max() {
        int max = -1;
        for(Integer state: v_decided.keySet()) {
            if(max < state) max = state;
        } return max;
    }

    /**
     * Min() should return one more than the minimum among z_i,
     * where z_i is the highest number ever passed
     * to Done() on peer i. A peers z_i is -1 if it has
     * never called Done().

     * Paxos is required to have forgotten all information
     * about any instances it knows that are < Min().
     * The point is to free up memory in long-running
     * Paxos-based servers.

     * Paxos peers need to exchange their highest Done()
     * arguments in order to implement Min(). These
     * exchanges can be piggybacked on ordinary Paxos
     * agreement protocol messages, so it is OK if one
     * peers Min does not reflect another Peers Done()
     * until after the next instance is agreed to.

     * The fact that Min() is defined as a minimum over
     * all Paxos peers means that Min() cannot increase until
     * all peers have been heard from. So if a peer is dead
     * or unreachable, other peers Min()s will not increase
     * even if all reachable peers call Done. The reason for
     * this is that when the unreachable peer comes back to
     * life, it will need to catch up on instances that it
     * missed -- the other peers therefore cannot forget these
     * instances.
     */
    public int Min() {
        done_lowest_lock.lock();
        int min=n_done_lowest[0];
        for(int i : n_done_lowest) { if(i < min) min = i;}
        done_lowest_lock.unlock();
        return min + 1;
    }

    /**
     * The application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(int seq) {

        // if status() seq < Min(), return forgotten
        //if(seq < Min()) return new retStatus(State.Forgotten, v_decided.get(seq).value);

        // for (int s: v_decided.keySet()) {
        //     PeerState p = getState(s);
        //     System.out.println("Sequence " + s + ": " +  p.value);
        // }
        if (v_decided.get(seq) == null) return new retStatus(State.Pending, null);

        return new retStatus(v_decided.get(seq).state, v_decided.get(seq).value);
    }

    /**
     * helper class for Status() return
     */
    public class retStatus {
        public State state;
        public Object v;

        public retStatus(State state, Object v) {
            this.state = state;
            this.v = v;
        }
    }

    /**
     * Tell the peer to shut itself down.
     * For testing.
     * Please don't change these four functions.
     */
    public void Kill() {
        this.dead.getAndSet(true);
        if (this.registry != null) {
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
            } catch (Exception e) {
                System.out.println("None reference");
            }
        }
    }

    public boolean isDead() {
        return this.dead.get();
    }

    public void setUnreliable() {
        this.unreliable.getAndSet(true);
    }

    public boolean isunreliable() {
        return this.unreliable.get();
    }
}
