//UT-EID=


import java.util.*;
import java.util.concurrent.*;

public class PSort{

  public static void parallelSort(int[] A, int begin, int end){

    int n = end - begin;
    if(n <= 16) {
      // insertion sort
      for(int i = begin; i < n; i++) {
        int j = 1;
        while(j > begin && A[j - 1] > A[j]) {
          int temp = A[j - 1];
          A[j - 1] = A[j];
          A[j] = temp;
          j -= 1;
        }
      }
    }

    else {
      int middle = (end - begin) / 2;
      parallelSort(A, begin, middle);
      parallelSort(A, middle + 1, end);

    }


  }
}

class ForkJoinPSort extends ForkJoinPool {

}

class RunnablePSort implements Runnable {

  @Override
  public void run() {
    // TODO Auto-generated method stub
    
  }
  
}

