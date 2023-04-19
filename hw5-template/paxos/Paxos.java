package paxos;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is the main class you need to implement paxos instances.
 * It corresponds to a single Paxos peer.
 */
public class Paxos implements PaxosRMI, Runnable {

    static AtomicInteger global_proposal_number = new AtomicInteger(0);

    ReentrantLock mutex;
    String[] peers;             // hostnames of all peers
    int[] ports;                // ports of all peers
    int me;                     // this peer's index into peers[] and ports[]

    Registry registry;
    PaxosRMI stub;
    
    // Broadcast to peers once true
    boolean decided = false;
    
    // Used by a process to generate unique proposal numbers when proposing values
    int n_seq;

    // n represents a proposal number
    int n_prepare_highest,
        n_accept_highest,
        n_done_highest,
        n_proposal;
    // v corresponds to the value to be proposed by this paxos instance
    Object v;
    Object n_accept_highest_value;

    State state;
    AtomicBoolean dead,
                  unreliable;   // for testing
    
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

        n_seq = n_prepare_highest = n_proposal = 0;
        state = State.Pending;


        // register peers, do not modify this part
        try {
            System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
            registry = LocateRegistry.createRegistry(this.ports[this.me]);
            stub = (PaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("Paxos", stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Paxos(int me, String[] peers, int[] ports, Object value) {
        this(me, peers, ports);
        this.v = value;
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
        Paxos newPaxos = new Paxos(seq, peers, ports, value);
        new Thread(newPaxos).start();
    }
    
    /*
     * PROPOSER
     */
    @Override
    public void run() {
        while (!decided) {
            // For process i of N, n_send_propose = i + (sequence * N)
            // Increase number
            int n_send_propose = global_proposal_number.incrementAndGet();

            int num_prepare = 0, 
                num_accept = 0;
            
            Request request = new Request(
                    n_send_propose, 
                    n_done_highest, 
                    v);

            // Send to all instances, check if receive ok from more than half of array length
            for (int i = 0; i < peers.length; i++) {

                // Send prepare via RMI and look for OK response
                Response r_prepare = Call("Prepare", request, me);
                if (r_prepare.type == MessageType.PREPARE_OK) num_prepare++;
                else if(r_prepare.type == MessageType.PREPARE_REJECT) {
                    // Rejected message, so adjust the highest proposed n seen
                    n_prepare_highest = r_prepare.n_proposal;
                }
                // Now check if n done needs to be replaced
                if (r_prepare.n_done > n_done_highest) n_done_highest = r_prepare.n_done;
            }

            // Accept request in case n and v have changed
            Request a_request = new Request(
                    n_send_propose, 
                    n_done_highest, 
                    v);

            // Check for majority OK, then move to accept
            if (num_prepare > Integer.valueOf(peers.length / 2)) {
                for (int i = 0; i < peers.length; i++) {
                    Response r_accept = Call("Accept", a_request, me);

                    if (r_accept.type == MessageType.ACCEPT_OK) num_accept++;
                    else if (r_accept.type == MessageType.ACCEPT_REJECT) {
                        // Rejected message, so adjust the highest accepted n seen
                        n_accept_highest = r_accept.n_accept;
                    }
                }
                if (num_accept > Integer.valueOf(peers.length / 2)) {
                    for (int i = 0; i < peers.length; i++) {
                    // Decide request in case n and v have changed
                    Request d_request = new Request(
                            n_send_propose, 
                            n_done_highest, 
                            v);

                    for (int j = 0; j < peers.length; j++) {
                        Call("Decide", d_request, me);
                    }
                }
            }
        } 
        }
    }

    /*
     * ACCEPTOR
     */

    // RMI Handler for prepare requests
    public Response Prepare(Request req) {

        Response r;

        if (req.n_proposal > n_prepare_highest) {
            // PREPARE OK
            n_prepare_highest = req.n_proposal;
            r = new Response(
                MessageType.PREPARE_OK,
                req.n_proposal,
                n_accept_highest, 
                n_done_highest,
                n_accept_highest_value);
            return r;
        } else {
            // PREPARE REJECT
            r = new Response(
                MessageType.PREPARE_REJECT, 
                req.n_proposal,
                n_accept_highest, 
                n_done_highest,
                n_accept_highest_value);
            return r;
        }
    }

    // RMI Handler for accept requests
    public Response Accept(Request req) {

        Response r;

        if (req.n_proposal >= n_prepare_highest) {
            n_prepare_highest = req.n_proposal;         // n_p = n
            n_accept_highest = req.n_proposal;          // n_a = n
            n_accept_highest_value = req.value;         // v_a = v
            
            // ACCEPT OK
            r = new Response(
                MessageType.ACCEPT_OK, 
                req.n_proposal,
                n_accept_highest, 
                n_done_highest,
                n_accept_highest_value);
            return r;
        } else {
            // ACCEPT REJECT
            r = new Response(
                MessageType.ACCEPT_REJECT,
                req.n_proposal,
                n_accept_highest, 
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
        // TODO
        return null;
    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
        // Your code here
        // for(Paxos current: instances) {
        //     if(current.seq <= min) {
        //         current.Kill();
        //     }
        // }

        //TODO send the highest Done argument supplied by local application
    }

    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max() {
        // TODO
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
        //TODO: Check local done state
        return -1;
    }

    /**
     * The application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(int seq) {
        return new retStatus(state, v);
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
