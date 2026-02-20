package com.aditya.parivarpocket.util;

import com.aditya.parivarpocket.model.JobOpportunity;

import java.util.List;

/**
 * Utility class providing sorting algorithms for job listings.
 *
 * <p>Currently contains a Bubble Sort implementation for educational reference.
 * The application uses Java's built-in sort (via Comparator) for actual sorting.
 */
public class JobSorting {

    private JobSorting() {
        // Utility class — not instantiable
    }
    /**
     * Sorts a list of {@link JobOpportunity} objects by their publication date
     * (newest first) using the Bubble Sort algorithm.
     *
     * <p><b>Time complexity:</b> O(n²) average and worst case.<br>
     * <b>Space complexity:</b> O(1) — in-place sort.<br>
     * <b>Stable:</b> Yes.
     *
     * <p>This implementation is provided for reference and is <em>not</em>
     * called anywhere in the application. The app uses
     * {@code List.sort(Comparator)} instead.
     *
     * @param jobs the list of job opportunities to sort (modified in-place)
     */
    public static void bubbleSortByDateDescending(List<JobOpportunity> jobs) {
        if (jobs == null || jobs.size() <= 1) {
            return;
        }

        int n = jobs.size();
        boolean swapped;

        for (int i = 0; i < n - 1; i++) {
            swapped = false;

            for (int j = 0; j < n - 1 - i; j++) {
                JobOpportunity a = jobs.get(j);
                JobOpportunity b = jobs.get(j + 1);

                // Compare by pub_date_ts_milli descending (newer jobs first)
                if (a.getPubDateTsMilli() < b.getPubDateTsMilli()) {
                    // Swap
                    jobs.set(j, b);
                    jobs.set(j + 1, a);
                    swapped = true;
                }
            }

            // If no swap occurred in this pass, the list is already sorted
            if (!swapped) {
                break;
            }
        }
    }

    /**
     * Sorts a list of {@link JobOpportunity} objects alphabetically by title
     * (A → Z) using the Bubble Sort algorithm.
     *
     * <p>Same complexity characteristics as
     * {@link #bubbleSortByDateDescending(List)}.
     *
     * @param jobs the list of job opportunities to sort (modified in-place)
     */
    public static void bubbleSortByTitleAscending(List<JobOpportunity> jobs) {
        if (jobs == null || jobs.size() <= 1) {
            return;
        }

        int n = jobs.size();
        boolean swapped;

        for (int i = 0; i < n - 1; i++) {
            swapped = false;

            for (int j = 0; j < n - 1 - i; j++) {
                JobOpportunity a = jobs.get(j);
                JobOpportunity b = jobs.get(j + 1);

                if (a.getTitle().compareToIgnoreCase(b.getTitle()) > 0) {
                    jobs.set(j, b);
                    jobs.set(j + 1, a);
                    swapped = true;
                }
            }

            if (!swapped) {
                break;
            }
        }
    }
}
