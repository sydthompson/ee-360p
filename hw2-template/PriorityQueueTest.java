public class PriorityQueueTest implements Runnable{
    
    static boolean barrierBroken = false;
    final PriorityQueue priorityQueue;
    String name;
    int priority;

    PriorityQueueTest(PriorityQueue priorityQueue, String name, int priority) {
        this.priorityQueue = priorityQueue;
        this.name = name;
        this.priority = priority;
    }

    public void run() {
        System.out.println(String.format("Added [%s, %d] at: %d", name, priority, priorityQueue.add(name, priority)));
//        if(priority % 3==0) {
//            System.out.println("removed first: " + priorityQueue.getFirst());
//        }
        // System.out.println(String.format("Added [%s, %d] at: %d", "temp", 2, priorityQueue.add("temp", 2)));
        // priorityQueue.print();

        // System.out.println(String.format("Added [%s, %d] at: %d", "test2", 1, priorityQueue.add("test2", 1)));
        // priorityQueue.print();

        // System.out.println(String.format("Added [%s, %d] at: %d", "s", priority, priorityQueue.add("s", priority)));
        // priorityQueue.print();

        // System.out.println(String.format("Added [%s, %d] at: %d", "temp3", 2, priorityQueue.add("temp3", 2)));
        // priorityQueue.print();

        // System.out.println(String.format("Added [%s, %d] at: %d", "test23", 1, priorityQueue.add("test23", 1)));
        // priorityQueue.print();

        // System.out.println(String.format("Added [%s, %d] at: %d", "t", priority, priorityQueue.add("t", priority)));
        // priorityQueue.print();

        // System.out.println(String.format("Added [%s, %d] at: %d", "temp4", 2, priorityQueue.add("temp4", 2)));
        // priorityQueue.print();

        // System.out.println(String.format("Added [%s, %d] at: %d", "test24", 1, priorityQueue.add("test24", 1)));
        // priorityQueue.print();
    }

    public static void main(String[] args) throws InterruptedException {
        PriorityQueue priorityQueue = new PriorityQueue(200);
        int numParties = 150;
        Thread[] t = new Thread[numParties];
        for (int i = 0; i < numParties; ++i) {
			t[i] = new Thread(new PriorityQueueTest(priorityQueue, "" + i, (int) (Math.random() * 9) + 1));
		}
		for (int i = 0; i < numParties; ++i) {
			t[i].start();
		}
		for (int i = 0; i < numParties; ++i) {
			t[i].join();
            //priorityQueue.print();
            System.out.println(priorityQueue.size);
		}
    }
}
