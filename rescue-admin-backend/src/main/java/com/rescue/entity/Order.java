package com.rescue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 订单实体（兼容服务商端 + 车主端）
 */
@Data
@TableName("t_order")
public class Order {
    /**
     * 订单编号
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String orderNo;

    /**
     * 故障类型ID
     */
    private Integer faultTypeId;

    /**
     * 订单状态：pending-待处理，processing-处理中，completed-已完成，cancelled-已取消（你的原有字段）
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 服务商ID
     */
    private String supplierId;

    /**
     * 车主ID（关联车主表，用于归属校验）
     */
    private Long userId;

    private String address;

    /**
     * 救援地址纬度（前端选择地址时传的经纬度）
     */
    private Double latitude;

    /**
     * 救援地址经度
     */
    private Double longitude;

    /**
     * 订单备注（车主填写的额外说明）
     */
    private String remark;

    /**
     * 是否评价：0-未评价，1-已评价（关联评价表）
     */
    private Integer evaluateStatus;

    /**
     * 更新时间（记录状态变更时间）
     */
    private Date updateTime;

    private Date cancelTime;

    /**
     * 服务商当前位置-纬度
     */
    private Double supplierLat;

    /**
     * 服务商当前位置-经度
     */
    private Double supplierLng;
}