public class BathroomTest implements Runnable {

	final FairUnifanBathroom bathroom;
	final College mySchool;
	final Action myAction;

	enum College {
		UT,
		OU
	}

	enum Action {
		ENTER,
		LEAVE
	}

	public BathroomTest(FairUnifanBathroom bathroom, College mySchool, Action myAction) {
		this.bathroom = bathroom;
		this.mySchool = mySchool;
		this.myAction = myAction;
	}

	public void run() {
		try {
			if (mySchool == College.OU && myAction == Action.ENTER) bathroom.enterBathroomOU();
			else if (mySchool == College.OU && myAction == Action.LEAVE) bathroom.leaveBathroomOU();
			else if (mySchool == College.UT && myAction == Action.ENTER) bathroom.enterBathroomUT();
			else if (mySchool == College.UT && myAction == Action.LEAVE) bathroom.leaveBathroomUT();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		FairUnifanBathroom bathroom = new FairUnifanBathroom();
		// Tests mainly verify no hanging should occur
		// AKA no leaves will be called that do not correspond to an enter
		// Threads are started sequentially, so they should execute in the order they are joined

		/*
		 *
		 * 1. PoC for join and leave
		 * Simulate a 1-capacity bathroom
		 * Verify basic join and leave functionality
		 * No capacity testing
		 *
		 */
		// Test 1
		Thread[] t = new Thread[4];
		t[0] = new Thread(new BathroomTest(bathroom, College.UT, Action.ENTER));
		t[1] = new Thread(new BathroomTest(bathroom, College.OU, Action.ENTER));
		t[2] = new Thread(new BathroomTest(bathroom, College.UT, Action.LEAVE));
		t[3] = new Thread(new BathroomTest(bathroom, College.OU, Action.LEAVE));
		for (int i = 0; i < t.length; ++i) {
			t[i].start();
		}
		for (int i = 0; i < t.length; ++i) {
			t[i].join();
		}
		bathroom.bathroomStatus();
		System.out.println("Test 1 passed!");

		// Test 2
		Thread[] v = new Thread[15];
		for (int i = 0; i < 7; ++i) {
			v[i] = new Thread(new BathroomTest(bathroom, College.UT, Action.ENTER));
		}
		v[7] = new Thread(new BathroomTest(bathroom, College.OU, Action.ENTER));
		for (int i = 8; i < 15; ++i) {
			v[i] = new Thread(new BathroomTest(bathroom, College.UT, Action.LEAVE));
		}
		for (int i = 0; i < v.length; ++i) {
			v[i].start();
		}
		for (int i = 0; i < v.length; ++i) {
			v[i].join();
		}
		bathroom.bathroomStatus();
		System.out.println("Test 2 passed!");

		// Test 3
		Thread[] u = new Thread[16];
		for (int i = 0; i < 3; ++i) {
			u[i] = new Thread(new BathroomTest(bathroom, College.UT, Action.ENTER));
		}
		Thread.sleep(1000);
		for (int i = 3; i < 6; ++i) {
			u[i] = new Thread(new BathroomTest(bathroom, College.UT, Action.LEAVE));
		}
		Thread.sleep(1000);
		for (int i = 6; i < 9; ++i) {
			u[i] = new Thread(new BathroomTest(bathroom, College.OU, Action.ENTER));
		}
		Thread.sleep(1000);
		for (int i = 9; i < 11; ++i) {
			u[i] = new Thread(new BathroomTest(bathroom, College.UT, Action.ENTER));
		}
		Thread.sleep(1000);
		for (int i = 11; i < 14; ++i) {
			u[i] = new Thread(new BathroomTest(bathroom, College.OU, Action.LEAVE));
		}
		Thread.sleep(1000);
		for (int i = 14; i < 16; ++i) {
			u[i] = new Thread(new BathroomTest(bathroom, College.UT, Action.LEAVE));
		}
		for (int i = 0; i < u.length; ++i) {
			u[i].start();
		}
		for (int i = 0; i < u.length; ++i) {
			u[i].join();
		}
		bathroom.bathroomStatus();
		System.out.println("Test 3 passed!");
	}
}