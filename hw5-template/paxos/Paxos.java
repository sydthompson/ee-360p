package paxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;

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
    
    // Broadcast to peers once true
    boolean decided = false;
    
    // Used by a process to generate unique proposal numbers when proposing values

    // n represents a proposal timestamp
    int n_clock,
        n_prepare_highest,
        n_accept_highest,
        n_done_highest;

    // v corresponds to the value to be proposed by this paxos instance
    int seq;
    Object value;
    Object n_accept_highest_value;

    State state;
    AtomicBoolean dead,
                  unreliable;   // for testing

    int[] n_done_lowest;

    static ConcurrentHashMap<Integer, Object> v_decided = new ConcurrentHashMap<Integer, Object>();
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

        n_accept_highest = n_prepare_highest = n_done_highest = -1;
        state = State.Pending;

        n_done_lowest = new int[peers.length];
        for(int i =0; i < n_done_lowest.length; i++) { n_done_lowest[i]=-1; }
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
        if(seq < Min()) return;

        Paxos newPaxos = new Paxos(me, peers, ports);
        newPaxos.seq = seq;
        newPaxos.value = value;
        new Thread(newPaxos).start();
        System.out.println("Thread started");
    }
    
    /*
     * PROPOSER
     */
    @Override
    public void run() {
        n_clock = (seq + 1) * peers.length;
        while (state != State.Decided && state != State.Forgotten) {

            if(isDead()) return;

            n_clock += (me + 1);
            int highest_n = n_clock;

            int num_prepare = 0, 
                num_accept = 0;

            Request request = new Request(
                    seq,
                    highest_n, 
                    n_done_lowest[me],
                    n_done_highest, 
                    value);

            // Send to all instances, check if receive ok from more than half of array length
            for (int i = 0; i < peers.length; i++) {

                // Send prepare via RMI and look for OK response
                Response r_prepare = Call("Prepare", request, i);
                // System.out.println(String.format("My clock (P%d): %d, Peer response (P%d): %s",
                // me, highest_n, i, r_prepare.toString()));
                
                if (r_prepare != null){
                    if (r_prepare.type == MessageType.PREPARE_OK) num_prepare++;
                    // Now check if n done needs to be replaced
                    if (r_prepare.n_done > n_done_highest) n_done_highest = r_prepare.n_done;
                    n_done_lowest[i] = r_prepare.n_done_lowest;
                } 
            }

            // Check for majority OK, then move to accept
            if (num_prepare > Integer.valueOf(peers.length / 2)) {
                // Accept request in case n and v have changed
                Request a_request = new Request(
                        seq,
                        highest_n, 
                        n_done_lowest[me],
                        n_done_highest, 
                        value);

                for (int i = 0; i < peers.length; i++) {
                    Response r_accept = Call("Accept", a_request, i);
                    // System.out.println(String.format("My clock (P%d): %d, Peer response (P%d): %s",
                    // me, highest_n, i, r_accept.toString()));
                    if (r_accept != null) {
                        if (r_accept.type == MessageType.ACCEPT_OK) num_accept++;
                    }
                }

                // Majority OK, move to decide
                if (num_accept > Integer.valueOf(peers.length / 2)) {
                    System.out.println("Decision reached");
                    for (int i = 0; i < peers.length; i++) {
                    // Decide request in case n and v have changed
                        Request d_request = new Request(
                                seq,
                                highest_n,
                                n_done_lowest[me],
                                n_done_highest, 
                                value);
                        for (int j = 0; j < peers.length; j++) {
                            Call("Decide", d_request, i);
                        }
                    }

                    v_decided.put(seq, value);
                }
            } 
        }
    }

    /*
     * ACCEPTOR
     */

    // RMI Handler for prepare requests
    public Response Prepare(Request req) {
        if (req.seq != seq) return null;

        Response r;
        if (req.n_clock > n_prepare_highest) {
            // PREPARE OK
            n_prepare_highest = req.n_clock;
            r = new Response(
                MessageType.PREPARE_OK,
                n_clock,
                n_accept_highest, 
                n_done_lowest[me],
                n_done_highest,
                n_accept_highest_value);
            return r;
        } else {
            // PREPARE REJECT
            r = new Response(
                MessageType.PREPARE_REJECT, 
                n_clock,
                n_accept_highest, 
                n_done_lowest[me],
                n_done_highest,
                n_accept_highest_value);
            return r;
        }
    }

    // RMI Handler for accept requests
    public Response Accept(Request req) {
        if (req.seq != seq) return null;

        Response r;

        if (req.n_clock >= n_prepare_highest) {
            n_prepare_highest = req.n_clock;         // n_p = n
            n_accept_highest = req.n_clock;          // n_a = n
            n_accept_highest_value = req.value;      // v_a = v
            
            // ACCEPT OK
            r = new Response(
                MessageType.ACCEPT_OK, 
                n_clock,
                n_accept_highest, 
                n_done_lowest[me],
                n_done_highest,
                n_accept_highest_value);
            return r;
        } else {
            // ACCEPT REJECT
            r = new Response(
                MessageType.ACCEPT_REJECT,
                n_clock,
                n_accept_highest, 
                n_done_lowest[me],
                n_done_highest,
                n_accept_highest_value);
            return r;
        }
    }

    /*
     * WHEN DECISION REACHED
     */

    // RMI Handler for decide requests
    public Response Decide(Request req) {
        this.state = State.Decided;
        this.value = req.value;
        return new Response(
            MessageType.DECIDE_OK,
            n_clock,
            n_accept_highest, 
            n_done_lowest[me],
            n_done_highest,
            n_accept_highest_value
        );
    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
        n_done_lowest[me]=seq;
        int min_sequence = Min();
        // find out which 
        if(this.seq < min_sequence) this.state = State.Forgotten;
        //TODO send the highest Done argument supplied by local application
    }

    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max() {
        return n_prepare_highest;
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
        int min=n_done_lowest[0];
        for(int i : n_done_lowest) { if(i < min) min = i;}
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
        if(seq < Min()) return new retStatus(State.Forgotten, value);


        return new retStatus(state, value);
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
