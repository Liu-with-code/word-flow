package com.wordflow.module.word.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.word.entity.Word;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 单词 Mapper。
 */
@Mapper
public interface WordMapper extends BaseMapper<Word> {

    /**
     * 查询指定词书下的全部单词 ID（只取主键，避免读入数万条词条的全部字段）。
     *
     * @param bookId 词书 ID
     * @return 单词 ID 列表
     */
    @Select("SELECT id FROM learn_word WHERE book_id = #{bookId}")
    List<Long> selectIdsByBookId(@Param("bookId") Long bookId);
}
