package com.rescue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户登录实体（修改后）
 */
@Data
@TableName("t_user")
public class User {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号
     */
    private String account;

    /**
     * 密码（不加密）
     */
    private String password;

    /**
     * 身份类型：1-管理员，2-供应商
     */
    private Integer identityType;

    /**
     * 供应商ID（仅身份类型为2时有值）
     */
    private String supplierId;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private String phone;

    private String nickname;
}