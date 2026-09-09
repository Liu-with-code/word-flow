package com.wordflow.module.word.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.word.entity.Word;
import org.apache.ibatis.annotations.Mapper;

/**
 * 单词 Mapper。
 *
 * 模块职责：
 *   - learn_word 表基础 CRUD。
 * 你需要完成：
 *   - 按难度/级别组合查询的复杂 SQL 可加在这里。
 */
@Mapper
public interface WordMapper extends BaseMapper<Word> {
}

