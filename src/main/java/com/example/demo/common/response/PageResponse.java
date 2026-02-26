package com.example.demo.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;
    private boolean first;
    private boolean last;

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public static <T> PageResponse<T> of(List<T> content, long totalElements, int totalPages, int size, int number) {
        return PageResponse.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .size(size)
                .number(number)
                .first(number == 0)
                .last(number == totalPages - 1)
                .build();
    }
}
