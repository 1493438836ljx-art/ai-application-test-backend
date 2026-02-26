package com.example.demo.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应封装类
 * <p>
 * 用于封装分页查询的结果，包含数据列表和分页信息。
 * </p>
 *
 * @param <T> 数据类型
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** 当前页数据列表 */
    private List<T> content;

    /** 总记录数 */
    private long totalElements;

    /** 总页数 */
    private int totalPages;

    /** 每页大小 */
    private int size;

    /** 当前页码（从0开始） */
    private int number;

    /** 是否为第一页 */
    private boolean first;

    /** 是否为最后一页 */
    private boolean last;

    /**
     * 从Spring Data的Page对象创建分页响应
     *
     * @param page Spring Data分页对象
     * @param <T>  数据类型
     * @return 分页响应对象
     */
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

    /**
     * 手动构建分页响应
     *
     * @param content       数据列表
     * @param totalElements 总记录数
     * @param totalPages    总页数
     * @param size          每页大小
     * @param number        当前页码
     * @param <T>           数据类型
     * @return 分页响应对象
     */
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
