//UT-EID=


import java.util.*;
import java.util.concurrent.*;

public class PSort{

  public static void parallelSort(int[] A, int begin, int end){


  }

}

class ForkJoinPSort extends ForkJoinPool {

  private int[] arr;
  private int begin_idx;
  private int end_idk;

  public ForkJoinPSort(int[] A, int begin, int end) {
    arr = A;
    begin_idx = begin;
    end_idk = end;
  }

  public static void parallelSort(int[] A, int begin, int end){

    int pivot = new Random().nextInt(end - begin) + begin;

    if((end - begin) <= 16) {
      // insertion sort
      for(int i = begin; i < end; i++) {
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
      parallelSort(A, begin, pivot);
      parallelSort(A, pivot + 1, end);

    }


  }

}

class RunnablePSort implements Runnable {

  private int[] arr;
  private int begin_idx;
  private int end_idk;

  public RunnablePSort  (int[] A, int begin, int end) {
    arr = A;
    begin_idx = begin;
    end_idk = end;
  }

  public static void parallelSort(int[] A, int begin, int end){

    int pivot = new Random().nextInt(end - begin) + begin;

    if((end - begin) <= 16) {
      // insertion sort
      Arrays.sort(A, begin, end + 1);
      // for(int i = begin; i < end; i++) {
      //   int j = 1;
      //   while(j > begin && A[j - 1] > A[j]) {
      //     int temp = A[j - 1];
      //     A[j - 1] = A[j];
      //     A[j] = temp;
      //     j -= 1;
      //   }
      // }
    }

    else {
      RunnablePSort left = new RunnablePSort(A, begin, pivot - 1);
      RunnablePSort right = new RunnablePSort(A, pivot + 1, end);
      left.run();
      right.run();
    }
  }

  public int get_partition() {
    int m = begin_idx - 1;
    int x = arr[end_idk];
    for(int n = begin_idx; n < end_idk; n++) {
      if(arr[n] < x) {
        m++;
        int temp = arr[m];
        arr[m] = arr[n];
        arr[n] = temp;
      }
    }
    m++;
    int temp = arr[m];
    arr[m] = arr[end_idk];
    arr[end_idk] = temp;

    return m;

  }

  @Override
  public void run() {
    parallelSort(arr, begin_idx, end_idk);
  }
  
}

