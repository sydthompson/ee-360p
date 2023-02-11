// EID 1
// EID 2

import java.util.concurrent.locks.*;

public class FairUnifanBathroom {

	private int ticketNumber = 0;
	private int occupancy = 0;
	private final int capacity = 7;

	private final ReentrantLock bathroomLock = new ReentrantLock(true);

							// If awaited, UT is in the bathroom
	private final Condition notUT = bathroomLock.newCondition(),
							// If awaited, OU is in the bathroom
							notOU = bathroomLock.newCondition();

							// Lock out ALL fans when the bathroom is full
	private final Condition notFull = bathroomLock.newCondition();

	public void enterBathroomUT() throws InterruptedException{
		bathroomLock.lock();
		try {
			// Lock the bathroom for UT if you're the first to arrive
			if (occupancy == 0) notUT.await();

			occupancy++;
			if (occupancy == capacity) {
				// Wait until notFull is signaled by a leave
				notFull.await();
			}
		} finally {
			bathroomLock.unlock();
		}
	}
	
	public void enterBathroomOU() throws InterruptedException{
		bathroomLock.lock();
		try {
			//Lock the bathroom for OU if you're the first to arrive
			if (occupancy == 0) notOU.await();

			occupancy++;
			if (occupancy == capacity) {
				// Wait until notFull is signaled by a leave
				notFull.await();
			}
		} finally {
			bathroomLock.unlock();
		}
	}
	
	public void leaveBathroomUT() throws InterruptedException {
		bathroomLock.lock();
		try {
			occupancy--;
			// Let line know the bathroom is not at capacity
			if (occupancy != capacity) {
				notFull.signalAll();
			}
			// Let line know that UT fans have exited the bathroom
			if (occupancy == 0) {
				notUT.signalAll();
			}
		} finally {
			bathroomLock.unlock();
		}
	}

	public synchronized void leaveBathroomOU() throws InterruptedException{
		bathroomLock.lock();
		try {
			occupancy--;
			// Let line know the bathroom is not at capacity
			if (occupancy != capacity) {
				notFull.signalAll();
			}
			// Let line know that OU fans have exited the bathroom
			if (occupancy == 0) {
				notOU.signalAll();
			}
		} finally {
			bathroomLock.unlock();
		}
	}
}
	
