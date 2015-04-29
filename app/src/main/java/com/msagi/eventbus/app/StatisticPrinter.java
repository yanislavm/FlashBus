package com.msagi.eventbus.app;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility class to print statistics.
 *
 * @author yanislav.mihaylov
 */
public class StatisticPrinter {

    private static final String FILE_NAME = "statistic.txt";

    private static final String LINE_STATISTIC = "| %1$10s | %2$10d | %3$10d | %4$10d | %5$10d | %6$10d |\n";

    private static final String LINE_SEPARATOR = "-------------------------------------------------------------------------------\n";

    /**
     * Print statistics.
     *
     * @param context The context to use.
     * @param name    The name of the test.
     * @param count   The count of the events.
     * @param min     The minimum time.
     * @param max     The maximum time.
     * @param sum     The cumulated time.
     * @throws IOException If I/O error happens during printing stats.
     */
    public static void printStatistics(final Context context, final String name, final long count, final long min, final long max, final long sum) throws IOException {
        final byte[] bytes = String.format(LINE_STATISTIC, name, count, min, max, sum / count, sum).getBytes();
        writeReport(context, bytes);
    }

    private static void writeReport(final Context context, final byte[] bytes) throws IOException {
        final File file = new File(context.getExternalFilesDir(null), FILE_NAME);
        if (!file.exists()) {
            file.createNewFile();
        }

        final FileOutputStream writer = new FileOutputStream(file, true);
        writer.write(bytes);
        writer.flush();
        writer.close();
    }

    /**
     * Print line separator to the statistics file.
     *
     * @param context The context to use.
     * @throws IOException If I/O error happens during print
     * ing to the file.
     */
    public static void printLineSeparator(final Context context) throws IOException {
        writeReport(context, LINE_SEPARATOR.getBytes());
    }
}
