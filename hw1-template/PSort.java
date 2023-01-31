//UT-EID=


import java.util.*;
import java.util.concurrent.*;

public class PSort{

  public static void parallelSort(int[] A, int begin, int end){

      //(new RunnablePSort(A, begin, end)).run();

      (new ForkJoinPSort(A, begin, end)).compute();

  }

  static int partition(int[] arr, int begin, int end)
  {
    int pivot = arr[end];
    int i = (begin-1);

    for (int j = begin; j < end; j++) {
        if (arr[j] <= pivot) {
            i++;

            int temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }

    int temp = arr[i+1];
    arr[i+1] = arr[end];
    arr[end] = temp;

    return i+1;
  }

  static void insertionSort(int[] arr, int begin, int end) {
    for(int i = begin+1; i < end+1; i++) {
      int key = arr[i];
      int j = i - 1;

      while(j >= begin && arr[j] > key) {
        arr[j+1] = arr[j];
        j = j-1;
      }
      arr[j+1] = key;

    }
  }

}

class ForkJoinPSort extends RecursiveAction {

  private int[] arr;
  private int begin_idx;
  private int end_idx;

  public ForkJoinPSort(int[] A, int begin, int end) {
    arr = A;
    begin_idx = begin;
    end_idx = Math.min(end, A.length - 1);
  }

  public static void parallelSort(int[] A, int begin, int end){
      ForkJoinPool myPool = new ForkJoinPool();
      myPool.invoke(new ForkJoinPSort(A, begin, end));
      myPool.shutdown();

  }

  @Override
  protected void compute() {
    if( begin_idx < end_idx) {

      if(end_idx - begin_idx <= 16) {
        synchronized(arr) {
          PSort.insertionSort(arr, begin_idx, end_idx);
        }
      }
      else {
        int pivot = PSort.partition(arr, begin_idx, end_idx);

        invokeAll(new ForkJoinPSort(arr, begin_idx, pivot - 1), 
                  new ForkJoinPSort(arr, pivot + 1, end_idx));
      }
      
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
    end_idk = Math.min(end, A.length - 1);
  }

  public static void parallelSort(int[] A, int begin, int end){
    if(begin >= end) {
      return;
    } else if((end - begin) <= 16) {
      synchronized(A) {
        PSort.insertionSort(A, begin, end);
      }
      return;
    } else {

      int pivot = PSort.partition(A, begin, end);

      parallelSort(A, begin, pivot - 1);
      RunnablePSort right = new RunnablePSort(A, pivot + 1, end);
      right.run();
    }
  }
  
  @Override
  public void run() {
    parallelSort(arr, begin_idx, end_idk);
  }
  
}

