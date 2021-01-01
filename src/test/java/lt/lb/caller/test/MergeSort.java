package lt.lb.caller.test;

import lt.lb.caller.Caller;
import lt.lb.caller.CallerBuilder;

/**
 *
 * @author laim0nas100
 */
public class MergeSort {

    public static void merge(Integer[] arr, int l, int m, int r) {
        int n1 = m - l + 1;
        int n2 = r - m;

        int L[] = new int[n1];
        int R[] = new int[n2];

        for (int i = 0; i < n1; ++i) {
            L[i] = arr[l + i];
        }
        for (int j = 0; j < n2; ++j) {
            R[j] = arr[m + 1 + j];
        }

        int i = 0, j = 0;

        int k = l;
        while (i < n1 && j < n2) {
            if (L[i] <= R[j]) {
                arr[k] = L[i];
                i++;
            } else {
                arr[k] = R[j];
                j++;
            }
            k++;
        }

        while (i < n1) {
            arr[k] = L[i];
            i++;
            k++;
        }

        while (j < n2) {
            arr[k] = R[j];
            j++;
            k++;
        }
    }

    public static void sort(Integer[] arr, int l, int r) {
        if (l < r) {
            int m = (l + r) / 2;

            sort(arr, l, m);
            sort(arr, m + 1, r);

            merge(arr, l, m, r);
        }
    }

    public static Caller sortCaller(Integer[] arr, int l, int r) {
        if (l < r) {
            int m = (l + r) / 2;

            return new CallerBuilder()
                    .withDependencyCallable(() -> sortCaller(arr, l, m))
                    .withDependencyCallable(() -> sortCaller(arr, m + 1, r))
                    .toResultRunnable(() -> {
                        merge(arr, l, m, r);
                    });
        }
        return Caller.ofNull();
    }
}
