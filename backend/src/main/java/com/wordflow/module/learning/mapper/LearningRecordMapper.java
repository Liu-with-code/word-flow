package com.wordflow.module.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.learning.entity.LearningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 学习记录流水 Mapper：统计作答正确数、按日聚合、查询最近记录。
 */
@Mapper
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {

    /**
     * 统计用户答对次数。
     *
     * @param userId 用户 ID
     * @return 答对记录数
     */
    @Select("SELECT COUNT(*) FROM learn_record WHERE user_id = #{userId} AND is_correct = 1")
    long countCorrect(@Param("userId") Long userId);

    /**
     * 统计用户作答总次数。
     *
     * @param userId 用户 ID
     * @return 作答记录总数
     */
    @Select("SELECT COUNT(*) FROM learn_record WHERE user_id = #{userId}")
    long countTotal(@Param("userId") Long userId);

    /**
     * 近 N 天每日作答次数（用于统计页柱状图）。
     *
     * @param userId 用户 ID
     * @param since  起始时间（含）
     * @return 每行包含 day（日期）与 cnt（次数）
     */
    @Select("SELECT DATE(created_at) AS day, COUNT(*) AS cnt FROM learn_record "
            + "WHERE user_id = #{userId} AND created_at >= #{since} "
            + "GROUP BY DATE(created_at) ORDER BY day")
    List<Map<String, Object>> countByDay(@Param("userId") Long userId,
                                          @Param("since") LocalDateTime since);

    /**
     * 最近作答记录（不含 JOIN，由 Service 补单词名）。
     *
     * @param userId 用户 ID
     * @param limit  返回条数上限
     * @return 按作答时间倒序的记录列表
     */
    @Select("SELECT * FROM learn_record WHERE user_id = #{userId} "
            + "ORDER BY created_at DESC LIMIT #{limit}")
    List<LearningRecord> selectRecent(@Param("userId") Long userId, @Param("limit") int limit);
}
