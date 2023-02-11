// EID 1
// EID 2

import java.util.concurrent.locks.*;


public class FairUnifanBathroom {

	private int ticketNumber = 0, ticketToEnter = 0;
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
	private final Condition notMyTurn = bathroomLock.newCondition();

	public void enterBathroomUT() throws InterruptedException{
		bathroomLock.lock();
		try {
			int myTicket = ++ticketNumber;

			// Bathroom might already be full
			while ((occupancy == capacity) || (occupier == College.OU) || (myTicket - ticketToEnter > 7)) {
				// Wait until someone signals a leave
				// Wait if you are not next in line
				// OR OU is in the bathroom right now
				notMyTurn.await();
			}

			// Bathroom is now UT's if everyone has exited when this fan enters
			if (occupancy == 0) occupier = College.UT;
			occupancy++;
		} finally {
			System.out.println("Enter reached UT");
			bathroomLock.unlock();
		}
	}
	
	public void enterBathroomOU() throws InterruptedException{
		bathroomLock.lock();
		try {
			int myTicket = ++ticketNumber;

			// Bathroom might already be full
			//TODO: Hey this math may not be mathing
			while ((occupancy == capacity) || (occupier == College.UT) || (myTicket - ticketToEnter > 7)) {
				// Wait until someone signals a leave
				// Wait if you are not next in line
				// OR UT is in the bathroom right now
				notMyTurn.await();
			}

			if (occupancy == 0) occupier = College.OU;
			occupancy++;
		} finally {
			System.out.println("Enter reached OU");
			bathroomLock.unlock();
		}
	}
	
	public void leaveBathroomUT() throws InterruptedException {
		bathroomLock.lock();
		try {
			occupancy--;
			// Just make sure the college is assigned correctly and that the next ticket to enter is updated
			// Then notify all threads and they will check their ticket number again
			if (occupancy == 0) occupier = College.NONE;

			ticketToEnter++;
			notMyTurn.signalAll();
		} finally {
			System.out.println("Exit reached UT");
			bathroomLock.unlock();
		}
	}

	public synchronized void leaveBathroomOU() throws InterruptedException{
		bathroomLock.lock();
		try {
			occupancy--;
			// Just make sure the college is assigned correctly and that the next ticket to enter is updated
			// Then notify all threads and they will check their ticket number again
			if (occupancy == 0) occupier = College.NONE;

			ticketToEnter++;
			notMyTurn.signalAll();
		} finally {
			System.out.println("Exit reached OU");
			bathroomLock.unlock();
		}
	}

	public void bathroomStatus() {
		System.out.println("occupancy: " + occupancy + ", occupier: " + occupier.toString() + ", line: " + bathroomLock.getQueueLength());
	}


}
	
