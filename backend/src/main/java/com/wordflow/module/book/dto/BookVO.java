package com.wordflow.module.book.dto;

/**
 * 词书出参（含当前用户在该词书上的学习进度）。
 *
 * @param wordCount       词书收录的单词总数
 * @param learnedCount    已开始学习的单词数（进入过四步练习）
 * @param masteredCount   已掌握（通过全部复习轮次）的单词数
 * @param remainingCount  尚未学习的单词数
 * @param progressPercent 学习进度百分比 0-100
 * @param active          是否为当前用户选中的词书
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
        boolean active,
        long learnedCount,
        long masteredCount,
        long remainingCount,
        int progressPercent
) {
}
