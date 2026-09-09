package com.wordflow.module.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;

/**
 * 词书 Mapper。
 *
 * 模块职责：
 *   - learn_book 表基础 CRUD。
 * 你需要完成：
 *   - 若后续增加「词书收藏/订阅」多对多关系，请新增 user_book 表并在此扩展。
 */
@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
