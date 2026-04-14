package com.rescue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 系统配置实体
 */
@Data
@TableName("t_system_config")
public class SystemConfig {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台名称
     */
    private String platformName;

    /**
     * 客服电话
     */
    private String servicePhone;

    /**
     * 救援响应时限（分钟）
     */
    private Integer responseTime;

    /**
     * 平台公告
     */
    private String notice;

    /**
     * 更新时间
     */
    private Date updateTime;
}