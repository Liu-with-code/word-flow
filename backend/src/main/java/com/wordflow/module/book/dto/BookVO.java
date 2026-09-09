package com.wordflow.module.book.dto;

/**
 * 词书出参。
 *
 * @param active 是否为当前用户选中的词书
 */
public record BookVO(
        Long id,
        String name,
        String code,
        String level,
        String description,
        String coverColor,
        Integer wordCount,
        Integer sortNo,
        boolean active
) {
}
