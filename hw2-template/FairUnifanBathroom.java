// EID 1
// EID 2

import java.util.concurrent.locks.*;


public class FairUnifanBathroom {

	private int ticketNumber = 0, ticketToEnter = 1;
	private int occupancy = 0;
	private final int capacity = 7;

	enum College {
		UT,
		OU,
		NONE
	}

	private College occupier = College.NONE;

	private final ReentrantLock bathroomLock = new ReentrantLock(true);

							// Lock out ALL fans when the bathroom is full
	private final Condition notFull = bathroomLock.newCondition(),
							notMyTurn = bathroomLock.newCondition();

	public void enterBathroomUT() throws InterruptedException{
		bathroomLock.lock();
		try {
			int myTicket = ticketNumber++;

			// Bathroom might already be full
			while (occupancy == capacity) {
				// Wait until someone signals a leave
				notFull.await();
			}

			while ((occupier == College.OU) || (ticketToEnter != myTicket)) {
				// Wait if you are not next in line
				// OR OU is in the bathroom right now
				notMyTurn.await();
			}

			// Bathroom is now UT's if everyone has exited when this fan enters
			if (occupancy == 0) occupier = College.UT;
			occupancy++;

		} finally {
			bathroomLock.unlock();
		}
	}
	
	public void enterBathroomOU() throws InterruptedException{
		bathroomLock.lock();
		try {
			int myTicket = ticketNumber++;

			// Bathroom might already be full
			while (occupancy == capacity) {
				// Wait until someone signals a leave
				notFull.await();
			}

			while ((occupier == College.UT) || (ticketToEnter != myTicket)) {
				// Wait if you are not next in line
				// OR UT is in the bathroom right now
				notMyTurn.await();
			}

			if (occupancy == 0) occupier = College.OU;
			occupancy++;

		} finally {
			bathroomLock.unlock();
		}
	}
	
	public void leaveBathroomUT() throws InterruptedException {
		bathroomLock.lock();
		try {
			occupancy--;
			// Let line know the bathroom is not at capacity
			if (occupancy != capacity) notFull.signalAll();
			// Just make sure the college is assigned correctly and that the next ticket to enter is updated
			// Then notify all threads and they will check their ticket number again
			if (occupancy == 0) occupier = College.NONE;

			ticketToEnter++;
			notMyTurn.notifyAll();

		} finally {
			bathroomLock.unlock();
		}
	}

	public synchronized void leaveBathroomOU() throws InterruptedException{
		bathroomLock.lock();
		try {
			occupancy--;
			// Let line know the bathroom is not at capacity
			if (occupancy != capacity) notFull.signalAll();
			// Just make sure the college is assigned correctly and that the next ticket to enter is updated
			// Then notify all threads and they will check their ticket number again
			if (occupancy == 0) occupier = College.NONE;

			ticketToEnter++;
			notMyTurn.notifyAll();

		} finally {
			bathroomLock.unlock();
		}
	}

	public synchronized String bathroomStatus() throws InterruptedException{

	}
}
	
