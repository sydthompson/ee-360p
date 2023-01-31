// EID: mhv352,

import java.util.*;
import java.util.concurrent.*;

public class PMerge {
    /* Notes:
     * Arrays A and B are sorted in the ascending order
     * These arrays may have different sizes.
     * Array C is the merged array sorted in the descending order
     */
    public static void parallelMerge(int[] A, int[] B, int[] C, int numThreads) {

        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            // Find where elems of A belong in C
            for (Integer i=0; i<A.length; i++) {
                ThreadMerge tA = new ThreadMerge(B, A[i]);
                Future<Integer> index_in_b = threadPool.submit(tA);
                Integer index_in_c = C.length - 1 - (i + index_in_b.get());
                C[index_in_c] = A[i];
            }

            // Find where elems of B belong in C
            for (Integer i=0; i<B.length; i++) {
                ThreadMerge tB = new ThreadMerge(A, B[i]);
                Future<Integer> index_in_a = threadPool.submit(tB);

                // Lazy tiebreaker in case a duplicate exists in A
                Integer index_in_c = C.length - 1 - (i + index_in_a.get());
                synchronized (A) {
                    for (int j=0; j<A.length; j++) {
                        if (A[j] == B[i]) index_in_c -= 1;
                    }
                    C[index_in_c] = B[i];
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}

class ThreadMerge implements Callable<Integer> {

    int[] arr;
    int insert;

    public ThreadMerge(int[] arr, int insert) {
        this.arr = arr;
        this.insert = insert;
    }

    public Integer call(){
        return this.findInsertIndex(this.arr, this.insert);
    }

    public Integer findInsertIndex(int[] arr, int n) {
        // Lower and upper bounds
        Integer low = 0;
        Integer high = arr.length - 1;
    
        while (low <= high) {
            // Determine mid value
            Integer mid = (Integer) low + ((high - low) / 2);

            if (arr[mid] == n) {
                // If a duplicate is found, just return the match index and handle tiebreaker outside
                return mid;
            }
            else if (arr[mid] < n) {
                low = mid+1;
            }
            else {
                high = mid-1;
            }
        }

        // High pointer moved to the left of insert position if no duplicate, so shift by 1
        return high + 1;
    }
}