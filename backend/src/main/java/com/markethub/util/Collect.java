package com.markethub.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Reusable generic helpers for the collections the dashboards need.
 *
 * <p>The wildcards follow the "PECS" rule: a method that only <em>reads</em> elements takes
 * {@code ? extends T}; a function that only <em>consumes</em> them takes {@code ? super T}.</p>
 */
public final class Collect {

    private Collect() { }

    /** Groups items by a key, keeping first-seen key order. */
    public static <T, K> Map<K, List<T>> groupBy(Collection<? extends T> items,
                                                 Function<? super T, ? extends K> keyOf) {
        Map<K, List<T>> groups = new LinkedHashMap<>();
        for (T item : items) {
            groups.computeIfAbsent(keyOf.apply(item), key -> new ArrayList<>()).add(item);
        }
        return groups;
    }

    /** Adds up a money value per key; result is sorted by key. */
    public static <T, K extends Comparable<? super K>> SortedMap<K, BigDecimal> sumBy(
            Collection<? extends T> items,
            Function<? super T, ? extends K> keyOf,
            Function<? super T, BigDecimal> valueOf) {
        SortedMap<K, BigDecimal> totals = new TreeMap<>();
        for (T item : items) {
            totals.merge(keyOf.apply(item), valueOf.apply(item), BigDecimal::add);
        }
        return totals;
    }

    /** Counts items per key; result is sorted by key. */
    public static <T, K extends Comparable<? super K>> SortedMap<K, Integer> countBy(
            Collection<? extends T> items, Function<? super T, ? extends K> keyOf) {
        SortedMap<K, Integer> counts = new TreeMap<>();
        for (T item : items) {
            counts.merge(keyOf.apply(item), 1, Integer::sum);
        }
        return counts;
    }

    /** First {@code n} items after sorting with the comparator (the input is not modified). */
    public static <T> List<T> topN(Collection<? extends T> items, Comparator<? super T> order, int n) {
        List<T> copy = new ArrayList<>(items);
        copy.sort(order);
        return new ArrayList<>(copy.subList(0, Math.min(Math.max(n, 0), copy.size())));
    }

    /** Sum of money values; empty collection gives zero. */
    public static <T> BigDecimal sum(Collection<? extends T> items, Function<? super T, BigDecimal> valueOf) {
        BigDecimal total = BigDecimal.ZERO;
        for (T item : items) {
            total = total.add(valueOf.apply(item));
        }
        return total;
    }
}
