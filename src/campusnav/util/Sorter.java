package campusnav.util;

import campusnav.model.Location;
import java.util.ArrayList;

/** Insertion sort and binary search written by hand. */
public class Sorter {

    /** Insertion sort of locations by name (A to Z, ignoring case). */
    public static void sortByName(ArrayList<Location> list) {
        for (int i = 1; i < list.size(); i++) {
            Location key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j).compareTo(key) > 0) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    /** Binary search in a list that is already sorted with sortByName. Returns the index or -1. */
    public static int binarySearchByName(ArrayList<Location> sorted, String name) {
        int low = 0;
        int high = sorted.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int cmp = sorted.get(mid).getName().compareToIgnoreCase(name);
            if (cmp == 0) {
                return mid;
            } else if (cmp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    /** Insertion sort of location ids by the distance array (nearest first). */
    public static void sortByDistance(ArrayList<Integer> ids, double[] dist) {
        for (int i = 1; i < ids.size(); i++) {
            int key = ids.get(i);
            int j = i - 1;
            while (j >= 0 && dist[ids.get(j)] > dist[key]) {
                ids.set(j + 1, ids.get(j));
                j--;
            }
            ids.set(j + 1, key);
        }
    }
}
