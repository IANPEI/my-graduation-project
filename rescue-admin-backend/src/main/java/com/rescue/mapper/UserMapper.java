package com.rescue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rescue.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据账号查询用户
     */
    User selectByAccount(String account);


}