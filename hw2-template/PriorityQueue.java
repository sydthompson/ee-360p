import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


// st34596
// mhv352

public class PriorityQueue {

        int maxSize;
        Lock capacityLock;
        int size;
        Node head;
        Condition notEmpty;
        Condition notFull;

	public PriorityQueue(int maxSize) {
        // Creates a Priority queue with maximum allowed size as capacity
                this.maxSize = maxSize;
                this.capacityLock = new ReentrantLock();
                this.notEmpty = capacityLock.newCondition();
                this.notFull = capacityLock.newCondition();
                this.head = null;
                this.size = 0;
	}

	public int add(String name, int priority) {
        // Adds the name with its priority to this queue.
        // Returns the current position in the list where the name was inserted;
        // otherwise, returns -1 if the name is already present in the list.
        // This method blocks when the list is full.
                int idx=0;
                capacityLock.lock();
                try {
                        if(search(name) != -1) {
                                return -1;
                        }
                        if(maxSize == size) {
                                notFull.await();
                        }
                        capacityLock.unlock();

                        Node toInsert = new Node(name, priority, null);
                        Node current = head;

                        current.lock.lock();

                        if(head == null) {
                                head = toInsert;
                                current.lock.unlock();
                        }

                        else {
                                boolean inserted = false;
                                Node next = head.next;
                                while(next != null && !inserted) {
                                        try {
                                                next.lock.lock();
                                        } finally {
                                                if(next.priority < priority) {
                                                        current.next = toInsert;
                                                        toInsert.next = next;
                                                        inserted = true;
                                                        size += 1;
                                                        next.lock.unlock();
                                                }
                                                current.lock.unlock(); 
                                        }

                                        current = next;
                                        next = current.next;
                                        idx += 1;
                                }
                                if(!inserted) {
                                        idx += 1;
                                        current.next = toInsert;
                                        current.lock.unlock();
                                }
                        }
                        capacityLock.lock();
                        notEmpty.signal();
                        capacityLock.unlock();

                } catch(Exception e) {
                        System.out.println(e.getStackTrace());
                } 
                return idx;
	}

	public int search(String name) {
        // Returns the position of the name in the list;
        // otherwise, returns -1 if the name is not found.
                int idx=0;
                try {
                        capacityLock.lock();
                        if(0 == size) {
                                capacityLock.unlock();
                                return -1;
                        }
                        capacityLock.unlock();

                        Node current = head;
                        current.lock.lock();
                        
                        boolean found = false;
                        Node next = head.next;
                        while(next != null && !found) {
                                try {
                                        next.lock.lock();
                                } finally {
                                        if(current.name.equals(name)) {
                                                found=true;
                                        } else {
                                                idx +=1;
                                        }
                                       current.lock.unlock(); 
                                }
                                current = next;
                                next = current.next;
                        }
                        if(!found && !current.name.equals(name)) {
                            idx = -1;
                        }
                        current.lock.unlock();

                } catch(Exception e) {
                        System.out.println(e.getStackTrace());
                } 
                return idx;
	}

	public String getFirst() {
        // Retrieves and removes the name with the highest priority in the list,
        // or blocks the thread if the list is empty.
                String name = "";
                capacityLock.lock();
                try {
                        while(0 == size) {
                                notEmpty.await();
                        }

                        head.lock.lock();
                        name=head.name;

                        capacityLock.lock();

                        if(1 == size) {
                                head = null;
                        } else {
                                head.next.lock.lock();
                                head = head.next;
                                head.lock.unlock();
                                size -=1;
                        }  
                        notFull.signal();   
                        capacityLock.unlock();

                        
                } catch(Exception e) {
                        System.out.println(e.getStackTrace());
                }
                return name;
	}

        class Node {

                String name;
                int priority;
                Lock lock;
                Node next;

                public Node(String name, int priority, Node next) {
                        this.name = name;
                        this.priority = priority;
                        this.next = next;
                        this.lock = new ReentrantLock();
                }


        }
}

