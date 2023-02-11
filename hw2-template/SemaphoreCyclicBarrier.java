// EID 1
// EID 2

import java.util.concurrent.Semaphore; // for implementation using Semaphores

/* Use only semaphores to accomplish the required synchronization */
public class SemaphoreCyclicBarrier implements CyclicBarrier {

    // Cyclic barrier
    private Semaphore gate;
    private Semaphore method = new Semaphore(1);
    // Mutex controlling activation status
    private Semaphore active = new Semaphore(1);

    private int parties;
    private int counter = -1;

    public SemaphoreCyclicBarrier(int parties) throws InterruptedException {
        gate = new Semaphore(0);
        this.parties = parties;
        active.acquire();
    }

    /*
     * An active CyclicBarrier waits until all parties have invoked
     * await on this CyclicBarrier. If the current thread is not
     * the last to arrive then it is disabled for thread scheduling
     * purposes and lies dormant until the last thread arrives.
     * An inactive CyclicBarrier does not block the calling thread. It
     * instead allows the thread to proceed by immediately returning.
     * Returns: the arrival index of the current thread, where index 0
     * indicates the first to arrive and (parties-1) indicates
     * the last to arrive.
     */
    public int await() throws InterruptedException {
        // 2 semaphores
        // method - hold the method
        // gate - fence to wait for all threads in a round
        // Global mutex protects a run from being taken

        method.acquire();
        int index = counter++;
        boolean isActive = active.availablePermits() == 0;
        if (!isActive) {
            method.release();
            return -1;
        }

        if (counter == parties) {
            counter = 0;
            gate.release(parties-1);
            gate = new Semaphore(0);
            method.release();

            return index;
            // Round mutex releases when the amount of available permits is = to parties, gate reset to 0
        } else {
            Semaphore round_gate = gate;
            method.release();
            round_gate.acquire();

            return index;
        }
    }

    /*
     * This method activates the cyclic barrier. If it is already in
     * the active state, no change is made.
     * If the barrier is in the inactive state, it is activated and
     * the state of the barrier is reset to its initial value.
     */
    public void activate() throws InterruptedException {
        boolean isActive = active.availablePermits() == 0;
        if (isActive) return;

        active.acquire();
        gate = new Semaphore(0);
        method = new Semaphore(1);
        counter = 0;
    }

    /*
     * This method deactivates the cyclic barrier.
     * It also releases any waiting threads
     */
    public void deactivate() throws InterruptedException {
        boolean isActive = active.availablePermits() == 0;
        if (!isActive) return;

        gate = new Semaphore(0);
        method = new Semaphore(1);
        counter = 0;
        active.release();
    }
}
