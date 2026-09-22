package com.wordflow.module.progress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.progress.entity.WordProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 单词进度 Mapper。
 */
@Mapper
public interface WordProgressMapper extends BaseMapper<WordProgress> {

    /**
     * 按词书统计当前用户的「已开始学习」与「已掌握」单词数。
     *
     * 仅按账号维度统计，与当前选中词书无关，因此切换词书不影响任何一本书的进度。
     *
     * @param userId 用户 ID
     * @return 每行包含 bookId / started / mastered 三个字段
     */
    @Select("SELECT w.book_id AS bookId, "
            + "COUNT(*) AS started, "
            + "SUM(CASE WHEN p.status = 'COMPLETE' THEN 1 ELSE 0 END) AS mastered "
            + "FROM learn_progress p "
            + "JOIN learn_word w ON w.id = p.word_id "
            + "WHERE p.user_id = #{userId} "
            + "GROUP BY w.book_id")
    List<Map<String, Object>> countByBook(@Param("userId") Long userId);
}
