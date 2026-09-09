package com.wordflow.module.word.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单词实体，对应表 learn_word。
 *
 * 模块职责：
 *   - 词库基础数据：单词、音标、释义、词性、例句、难度、级别。
 * 你需要完成：
 *   - 词库导入：可写一个导入接口/脚本，批量导入 CSV 或 JSON 词库。
 */
@Data
@TableName("learn_word")
public class Word {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属词书ID */
    private Long bookId;

    private String word;

    private String phonetic;

    private String chinese;

    private String partOfSpeech;

    private String exampleEn;

    private String exampleZh;

    /** 难度 1-5 */
    private Integer difficulty;

    /** 词库级别：CET4/CET6/考研 */
    private String level;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
