package com.clickhouse.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This encapsulates a server reponse. Depending on concrete implementation, it
 * could be either an in-memory list or a wrapped input stream with
 * {@link ClickHouseDataProcessor} attached for deserialization. To get data
 * returned from server, depending on actual needs, you have 3 options:
 *
 * <ul>
 * <li>use {@link #records()} or {@link #stream()} to get deserialized
 * {@link ClickHouseRecord} one at a time</li>
 * <li>use {@link #firstRecord()} if you're certain that all you need is the
 * first {@link ClickHouseRecord}</li>
 * <li>use {@link #getInputStream()} or {@link #pipe(OutputStream, int)} if you
 * prefer to handle stream instead of deserialized data</li>
 * </ul>
 */
public interface ClickHouseResponse extends AutoCloseable, Serializable {
    /**
     * Gets list of columns.
     *
     * @return non-null list of column
     */
    List<ClickHouseColumn> getColumns();

    /**
     * Gets summary of this response. Keep in mind that the summary may change over
     * time until response is closed.
     *
     * @return non-null summary of this response
     */
    ClickHouseResponseSummary getSummary();

    /**
     * Gets input stream of the response. In general, this is the most
     * memory-efficient way for streaming data from server to client. However, this
     * also means additional work is required for deserialization, especially when
     * using a binary format.
     *
     * @return input stream for getting raw data returned from server
     */
    InputStream getInputStream();

    /**
     * Gets the first record only. Please use {@link #records()} instead if you need
     * to access the rest of records.
     *
     * @return the first record
     * @throws NoSuchElementException when there's no record at all
     * @throws UncheckedIOException   when failed to read data(e.g. deserialization)
     */
    default ClickHouseRecord firstRecord() {
        return records().iterator().next();
    }

    /**
     * Returns an iterable collection of records which can be walked through in a
     * foreach loop. Please pay attention that: 1) {@link UncheckedIOException}
     * might be thrown when iterating through the collection; and 2) it's not
     * supposed to be called for more than once.
     *
     * @return non-null iterable collection
     */
    Iterable<ClickHouseRecord> records();

    /**
     * Pipes the contents of this response into the given output stream.
     *
     * @param output     non-null output stream, which will remain open
     * @param bufferSize buffer size, 0 or negative value will be treated as 8192
     * @throws IOException when error occurred reading or writing data
     */
    default void pipe(OutputStream output, int bufferSize) throws IOException {
        ClickHouseChecker.nonNull(output, "output");

        if (bufferSize <= 0) {
            bufferSize = 8192;
        }

        byte[] buffer = new byte[bufferSize];
        int counter = 0;
        while ((counter = getInputStream().read(buffer, 0, bufferSize)) >= 0) {
            output.write(buffer, 0, counter);
        }

        // caller's responsibility to call output.flush() as needed
    }

    /**
     * Gets stream of records to process.
     *
     * @return stream of records
     */
    default Stream<ClickHouseRecord> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(records().iterator(),
                Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED), false);
    }

    @Override
    void close();

    /**
     * Checks whether the reponse has been closed or not.
     *
     * @return true if the response has been closed; false otherwise
     */
    boolean isClosed();
}
