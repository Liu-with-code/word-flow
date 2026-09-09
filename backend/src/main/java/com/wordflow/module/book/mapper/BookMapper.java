package com.wordflow.module.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;

/**
 * 词书 Mapper。
 *
 * 模块职责：
 *   - learn_book 表基础 CRUD。
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
