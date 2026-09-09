package com.wordflow.module.word.dto;

/**
 * 单词出参。
 */
public record WordVO(
        Long id,
        Long bookId,
        String word,
        String phonetic,
        String chinese,
        String partOfSpeech,
        String exampleEn,
        String exampleZh,
        Integer difficulty,
        String level
) {
}
