import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


// st34596
// mhv352

public class PriorityQueue {

        int maxSize;
        int size;

        ReentrantLock sizeLock = new ReentrantLock();

        Condition notFull = sizeLock.newCondition();
        Condition notEmpty = sizeLock.newCondition();

        // In case the priority levels of the dummy nodes will be used
        // Assign values outside of the priority range of 0-9
        Node dummyTail = new Node("", -1, null);
        Node dummyHead = new Node("", 10, dummyTail);

	public PriorityQueue(int maxSize) {
                // Creates a Priority queue with maximum allowed size as capacity
                this.maxSize = maxSize;
                this.size = 0;
	}

	public int add(String name, int priority) {
                // Adds the name with its priority to this queue.
                // Returns the current position in the list where the name was inserted;
                // otherwise, returns -1 if the name is already present in the list.
                // This method blocks when the list is full.

                sizeLock.lock();
                try {
                        if (search(name) != -1) return -1;

                        if (size == maxSize) notFull.await();
                        // Confirmed we can add the element, change the size while we still have the lock
                        size += 1;
                        notEmpty.signal();
                        sizeLock.unlock();

                        Node toInsert = new Node(name, priority, null);
                        int idx = 0;

                        Node prev = dummyHead;
                        prev.lock.lock();

                        Node curr = dummyHead.next;

                        while(curr.priority != -1 && curr.priority > priority) {
                                curr.lock.lock();
                                // Let go of previous node lock before acquiring next lock
                                prev.lock.unlock();

                                prev = curr;
                                curr = curr.next;
                                idx += 1;
                        }

                        // Change pointers
                        prev.next = toInsert;
                        toInsert.next = curr;
                        // Make sure we let go of the locks we need to once the insert happens
                        prev.lock.unlock();

                        return idx;
                } catch (Exception e) {
                        System.out.println(e);
                        e.printStackTrace();
                } finally {
                        if(dummyHead.lock.isHeldByCurrentThread())
                                dummyHead.lock.unlock();
                        if (dummyTail.lock.isHeldByCurrentThread())
                                dummyTail.lock.unlock();
                }
                return -1;
              
	}

	public int search(String name) {
        // Returns the position of the name in the list;
        // otherwise, returns -1 if the name is not found.
        dummyHead.lock.lock();
        try {
                int idx = 0;

                Node prev = dummyHead;
                // Current starts at the actual first item in the queue
                Node current = dummyHead.next;

                while(current.priority != -1) {
                        // Current and prev should be locked
                        current.lock.lock();
                        if (current.name.equals(name)) {
                                prev.lock.unlock();
                                current.lock.unlock();
                                return idx;
                        }
                        prev.lock.unlock();
                        prev = current;
                        current = current.next;
                        idx += 1;
                }
                prev.lock.unlock();

                return -1;
        } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
        } finally {
                if(dummyHead.lock.isHeldByCurrentThread())
                        dummyHead.lock.unlock();
        }
        return -1;
                
	}

	public String getFirst() {
        // Retrieves and removes the name with the highest priority in the list,
        // or blocks the thread if the list is empty.
                sizeLock.lock();
                try {
                        if (size == 0) notEmpty.await();
                        // Confirmed we can remove the element, change the size while we still have the lock
                        size -= 1;
                        notFull.signal();
                        sizeLock.unlock();

                        dummyHead.lock.lock();
                        Node toRemove = dummyHead.next;
                        toRemove.lock.lock();

                        Node afterRemove = toRemove.next;


                        dummyHead.next = afterRemove;

                        dummyHead.lock.unlock();
                        
                        return toRemove.name;
                } catch (Exception e) {
                        System.out.println(e);
                        e.printStackTrace();
                } finally {
                        if(dummyHead.lock.isHeldByCurrentThread())
                                dummyHead.lock.unlock();
                }
                return "";
	}

        void print() {
	            System.out.println("Printing PriorityQueue");
                Node curr = dummyHead.next;
                while(curr.priority != -1) {
                        System.out.println(curr.name + ", " + curr.priority);
                        curr = curr.next;
                }
        }

        class Node {

                String name;
                int priority;
                ReentrantLock lock;
                Node next;

                public Node(String name, int priority, Node next) {
                        this.name = name;
                        this.priority = priority;
                        this.next = next;
                        this.lock = new ReentrantLock();
                }


        }
}

