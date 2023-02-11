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
                int idx = 0;
                try {
                        if(search(name) != -1) {
                                return -1;
                        }
                        Node toInsert = new Node(name, priority, null);

                        capacityLock.lock();
                        if(size == 0) {
                                head = toInsert;
                                size += 1;
                                notEmpty.signal();
                                capacityLock.unlock();
                        } else {
                                capacityLock.unlock();

                                Node current = head;
                                current.lock.lock();
                                boolean inserted = false;
                                while(current != null && !inserted) {
                                        if(current.next != null) {
                                                current.next.lock.lock();
                                                if(current.next.priority < priority) {
                                                        toInsert.next = current.next;
                                                        current.next = toInsert;
                                                        idx -= 1;
                                                        inserted = true;
                                                } 
                                                Node next= current.next;
                                                current.lock.unlock();
                                                current = next;
                                                idx += 1;                                                

                                        } else {
                                                current.next = toInsert;
                                                current.lock.unlock();
                                                inserted = true;
                                        }
                                }

                                capacityLock.lock();
                                size += 1;
                                notEmpty.signal();
                                capacityLock.unlock();
                        }

               } catch(Exception e) {
                        System.out.println(e);
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
                        while(current != null) {
                                Node next = current.next;
                                if(current.name.equals(name)) {
                                        current.lock.unlock();
                                        return idx;
                                } idx += 1;
                                if(next != null) {
                                        next.lock.lock();
                                        current.lock.unlock();
                                        current = next;
                                } current = next;
                        }
                        return -1;

                } catch(Exception e) {
                        System.out.println(e);
                } 
                return idx;
	}

	public String getFirst() {
        // Retrieves and removes the name with the highest priority in the list,
        // or blocks the thread if the list is empty.
                String name = "";
                capacityLock.lock();
                try {
                        if(0 == size) {
                                notEmpty.await();
                        }

                        head.lock.lock();
                        name=head.name;


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
                        System.out.println(e);
                }
                return name;
	}

        public void print() {
                Node current = head;
                while(current != null) {
                        System.out.println(current.name + " " + current.priority + ", ");
                        current = current.next;
                }
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

