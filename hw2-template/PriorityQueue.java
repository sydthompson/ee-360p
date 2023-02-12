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
        ReadWriteLock sizeLock = new ReentrantReadWriteLock();
        Lock readSizeLock = sizeLock.readLock();
        Lock writeSizeLock = sizeLock.writeLock();

        Condition notFull = writeSizeLock.newCondition();
        Condition notEmpty = writeSizeLock.newCondition();

        Node dummyHead = new Node("", 0, null);


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

                readSizeLock.lock();
                dummyHead.lock.lock();
                try {
                        if(search(name) != -1) {
                                return -1;
                        }
                        System.out.println("done with search");


                        if(size == maxSize) {
                                notFull.await();
                        }
                        readSizeLock.unlock();

                        Node toInsert = new Node(name, priority, null);
                        int idx = 0;


                        Node prev = dummyHead;
                        prev.lock.lock();

                        Node curr = dummyHead.next;

                        while(curr != null && curr.priority > priority) {
                                curr.lock.lock();
                                prev.lock.unlock();

                                prev = curr;
                                curr = curr.next;
                                idx += 1;
                                
                        }

                        prev.next = toInsert;
                        toInsert.next = curr;
                        

                        if(curr != null && curr.lock.isHeldByCurrentThread()) {
                                curr.lock.unlock();
                        }

                        writeSizeLock.lock();
                        size += 1;
                        notEmpty.signal();
                        writeSizeLock.unlock();
                        return idx;


                } catch (Exception e) {
                        System.out.println(e);
                        e.printStackTrace();

                } finally {
                        if(dummyHead.lock.isHeldByCurrentThread())
                                dummyHead.lock.unlock();

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
                        Node current = dummyHead.next;

                        while(current != null) {
                                current.lock.lock();
                                if(current.name.equals(name)) {
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
                        dummyHead.lock.unlock();
                }
                return -1;
                
	}

	public String getFirst() {
        // Retrieves and removes the name with the highest priority in the list,
        // or blocks the thread if the list is empty.
                dummyHead.lock.lock();
                readSizeLock.lock();
                try {
                        if(size == 0) {
                                notEmpty.await();
                        }
                        readSizeLock.unlock();

                        Node toRemove = dummyHead.next;
                        toRemove.lock.lock();

                        Node afterRemove = toRemove.next;


                        dummyHead.next = afterRemove;

                        writeSizeLock.lock();
                        size -= 1;
                        notFull.signal();
                        writeSizeLock.unlock();

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
                Node curr = dummyHead.next;
                while(curr != null) {
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

