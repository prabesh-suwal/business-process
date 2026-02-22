package com.cas.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Generic pagination wrapper.
 * Used as the {@code data} payload in {@link ApiResponse} for paginated
 * endpoints.
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * PagedData&lt;TaskDTO&gt; page = PagedData.of(tasks, 0, 10, 45);
 * return ApiResponse.success(page);
 * </pre>
 *
 * @param <T> the type of items in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedData<T> {

    /** Items in the current page. */
    private List<T> content;

    /** Current page number (0-based). */
    private int page;

    /** Requested page size. */
    private int size;

    /** Total number of matching items across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** Whether the content is sorted. */
    private boolean sorted;

    /** Whether the content list is empty. */
    private boolean empty;

    /**
     * Create a PagedData from a content list and pagination metadata.
     *
     * @param content       items in the current page
     * @param page          current page number (0-based)
     * @param size          page size
     * @param totalElements total matching items
     * @return populated PagedData instance
     */
    public static <T> PagedData<T> of(List<T> content, int page, int size, long totalElements) {
        return PagedData.<T>builder()
                .content(content != null ? content : Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(size > 0 ? (int) Math.ceil((double) totalElements / size) : 0)
                .sorted(false)
                .empty(content == null || content.isEmpty())
                .build();
    }

    /**
     * Create a PagedData from a content list, pagination metadata, and sort info.
     */
    public static <T> PagedData<T> of(List<T> content, int page, int size, long totalElements, boolean sorted) {
        PagedData<T> result = of(content, page, size, totalElements);
        result.setSorted(sorted);
        return result;
    }

    /**
     * Create an empty PagedData.
     */
    public static <T> PagedData<T> empty(int page, int size) {
        return PagedData.<T>builder()
                .content(Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .sorted(false)
                .empty(true)
                .build();
    }
}
