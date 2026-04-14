package com.rescue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 服务商实体
 */
@Data
@TableName("t_supplier")
public class Supplier {
    /**
     * 服务商ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 服务商名称
     */
    private String name;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 状态：pending-待审核，enable-已启用，disable-已禁用
     */
    private String status;

    /**
     * 入驻时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}