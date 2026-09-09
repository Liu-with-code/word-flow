package com.wordflow.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wordflow.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 *
 * 模块职责：
 *   - 提供 sys_user 表的基础 CRUD。
 * 你需要完成：
 *   - 复杂统计查询可在此增加 @Select 方法。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

