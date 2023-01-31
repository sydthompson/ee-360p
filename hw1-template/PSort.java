//UT-EID=


import java.util.*;
import java.util.concurrent.*;

public class PSort{

  public static void parallelSort(int[] A, int begin, int end){

      (new RunnablePSort(A, begin, end)).run();

      //(new ForkJoinPSort(A, begin, end)).compute();

  }

  static void swap(int[] arr, int i, int j)
  {
      int temp = arr[i];
      arr[i] = arr[j];
      arr[j] = temp;
  }

  static int partition(int[] arr, int low, int high)
  {
      int pivot = arr[high];

      int i = (low - 1);

      for (int j = low; j <= high - 1; j++) {

          if (arr[j] < pivot) {
              i++;
              swap(arr, i, j);
          }
      }
      swap(arr, i + 1, high);
      return (i + 1);
  }

}

class ForkJoinPSort extends RecursiveAction {

  private int[] arr;
  private int begin_idx;
  private int end_idk;

  public ForkJoinPSort(int[] A, int begin, int end) {
    arr = A;
    begin_idx = begin;
    end_idk = Math.min(end, A.length - 1);
  }

  public static void parallelSort(int[] A, int begin, int end){

    int pivot = PSort.partition(A, begin, end);

    if((end - begin) <= 16) {
      // insertion sort
      Arrays.sort(A, begin, end);
    }

    else {

      ForkJoinPool poolJoin = new ForkJoinPool();

      ForkJoinPSort left = new ForkJoinPSort(A, begin, pivot);
      ForkJoinPSort right = new ForkJoinPSort(A, pivot + 1, end);

      poolJoin.invoke(left);
      poolJoin.invoke(right);

    }


  }

  @Override
  protected void compute() {
    parallelSort(arr, begin_idx, end_idk);
  }

}

class RunnablePSort implements Runnable {

  private int[] arr;
  private int begin_idx;
  private int end_idk;

  public RunnablePSort  (int[] A, int begin, int end) {
    arr = A;
    begin_idx = begin;
    end_idk = Math.min(end, A.length - 1);
  }

  public static void parallelSort(int[] A, int begin, int end){

    int pivot = PSort.partition(A, begin, end);
    if(begin == end) {
      return;
    }
    if((end - begin) <= 16) {
      // insertion sort
      Arrays.sort(A, begin, end);
      return;
  
    }

    else {
      RunnablePSort left = new RunnablePSort(A, begin, pivot);
      RunnablePSort right = new RunnablePSort(A, pivot + 1, end);
      left.run();
      right.run();
    }
  }

  public static int get_partition(int begin, int end, int[] A) {
    int m = begin - 1;
    end = Math.min(end, A.length-1);
    int x = A[end];
    for(int n = begin; n < end; n++) {
      if(A[n] < x) {
        m++;
        int temp = A[m];
        A[m] = A[n];
        A[n] = temp;
      }
    }
    m++;
    int temp = A[m];
    A[m] = A[end];
    A[end] = temp;

    return m;
  }
  

  @Override
  public void run() {
    parallelSort(arr, begin_idx, end_idk);
  }
  
}

