package com.rescue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 救援人员实体
 */
@Data
@TableName("t_staff")
public class Staff {
    /**
     * 人员ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 所属服务商ID
     */
    private String supplierId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 擅长救援类型
     */
    private String skill;

    /**
     * 状态：online-在线，offline-离线
     */
    private String status;

    /**
     * 入职时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}