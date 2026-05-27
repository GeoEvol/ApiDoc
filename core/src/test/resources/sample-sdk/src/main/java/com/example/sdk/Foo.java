package com.example.sdk;

import com.example.sdk.annotations.RequiresPermission;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Main sample API.
 *
 * <p>Body references {@link Bar helper type}, {@linkplain java.util.List plain list},
 * {@code code literal}, and an unknown inline tag {@customInline custom value}.</p>
 *
 * @param <T> item type
 * @since 1.0
 * @apiSince 3
 * @date 2026-05-21
 * @permission android.permission.INTERNET
 */
public class Foo<T extends Bar> implements ServiceContract {
    /**
     * Public constant value.
     *
     * @since 1.0
     */
    public static final String DEFAULT_NAME = "foo";

    /**
     * Creates a sample API instance.
     */
    public Foo() {
    }

    /**
     * Runs the sample operation.
     *
     * @param value value description
     * @param items item list
     * @return mapped result
     * @throws IllegalArgumentException if value is invalid
     * @see Bar
     */
    @RequiresPermission("sample.permission.RUN")
    public Map<String, T> run(String value, List<? extends T> items) throws IllegalArgumentException {
        return Map.of();
    }

    /**
     * Runs a varargs overload.
     *
     * @param values input values
     * @return number of values
     */
    public int run(String... values) {
        return values.length;
    }

    /**
     * Generic method with external JDK type references.
     *
     * @param timestamp timestamp from {@link Instant}
     * @param input input value
     * @param <R> result type
     * @return input value
     */
    public <R extends CharSequence> R convert(Instant timestamp, R input) {
        return input;
    }

    @Override
    public void close() {
    }
}
