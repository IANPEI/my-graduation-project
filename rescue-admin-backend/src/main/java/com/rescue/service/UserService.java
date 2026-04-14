package com.rescue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rescue.dto.RegisterDTO;
import com.rescue.entity.User;

/**
 * 用户服务接口（密码不加密）
 */
public interface UserService extends IService<User> {
    /**
     * 根据账号查询用户
     */
    User getUserByAccount(String account);

    /**
     * 验证用户登录（仅账号+密码，无加密）
     */
    User verifyLogin(String account, String password);


    /**
     * 用户注册
     * @param dto
     */
    void register(RegisterDTO dto);
}