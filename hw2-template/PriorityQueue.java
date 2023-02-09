import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


// st34596
// mhv352

public class PriorityQueue {

        int maxSize;
        Lock capacityLock;
        LinkedList<Node> queue;
        Condition notEmpty;
        Condition notFull;

	public PriorityQueue(int maxSize) {
        // Creates a Priority queue with maximum allowed size as capacity
                this.maxSize = maxSize;
                this.capacityLock = new ReentrantLock();
                this.notEmpty = capacityLock.newCondition();
                this.notFull = capacityLock.newCondition();
                this.queue = new LinkedList<>();
	}

	public int add(String name, int priority) {
        // Adds the name with its priority to this queue.
        // Returns the current position in the list where the name was inserted;
        // otherwise, returns -1 if the name is already present in the list.
        // This method blocks when the list is full.
                capacityLock.lock();
                try {
                        while(maxSize == queue.size()) {
                                notFull.await();
                        }
                        
                } catch(Exception e) {
                        System.out.println(e.getStackTrace());
                }finally {
                        capacityLock.unlock();
                }
                return 0;
	}

	public int search(String name) {
        // Returns the position of the name in the list;
        // otherwise, returns -1 if the name is not found.
                return 0;
	}

	public String getFirst() {
        // Retrieves and removes the name with the highest priority in the list,
        // or blocks the thread if the list is empty.
                return "";
	}

        class Node {

                String name;
                int priority;
                Lock lock;

                public Node(String name, int priority) {
                        this.name = name;
                        this.priority = priority;
                        this.lock = new ReentrantLock();
                }


        }
}

