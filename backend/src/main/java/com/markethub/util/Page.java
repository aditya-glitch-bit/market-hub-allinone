package com.markethub.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * One page of a longer list (generic container).
 *
 * @param <T> element type
 */
public final class Page<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final int totalItems;

    private Page(List<T> items, int page, int size, int totalItems) {
        this.items = Collections.unmodifiableList(items);
        this.page = page;
        this.size = size;
        this.totalItems = totalItems;
    }

    /**
     * Slices {@code all} into the requested page. Out-of-range pages are empty rather than errors.
     *
     * @param page 1-based page number (values below 1 are treated as 1)
     * @param size items per page (clamped to 1..100)
     */
    public static <T> Page<T> of(List<? extends T> all, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(1, page);
        int from = (int) Math.min((long) (safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new Page<T>(new ArrayList<T>(all.subList(from, to)), safePage, safeSize, all.size());
    }

    /** Converts each element, keeping the paging information (e.g. Product to JSON map). */
    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = new ArrayList<>(items.size());
        for (T item : items) {
            mapped.add(mapper.apply(item));
        }
        return new Page<R>(mapped, page, size, totalItems);
    }

    public int getTotalPages() {
        return totalItems == 0 ? 0 : (totalItems + size - 1) / size;
    }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("items", items);
        view.put("page", page);
        view.put("size", size);
        view.put("totalItems", totalItems);
        view.put("totalPages", getTotalPages());
        return view;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotalItems() { return totalItems; }
}
