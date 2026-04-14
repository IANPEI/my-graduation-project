package com.rescue.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 订单详情DTO（包含故障类型名称）
 */
@Data
public class OrderDetailDTO {
    /**
     * 订单编号
     */
    @ExcelProperty("订单编号")
    private String orderNo;

    /**
     * 故障类型ID
     */
    @ExcelProperty("故障类型ID")
    private Integer faultTypeId;

    /**
     * 故障类型名称（从fault_type表关联查询）
     */
    @ExcelProperty("故障类型名称")
    private String faultTypeName;

    /**
     * 车主姓名
     */
    @ExcelProperty("车主姓名")
    private String nickname;

    /**
     * 联系电话
     */
    @ExcelProperty("联系电话")
    private String phone;

    /**
     * 救援地址
     */
    @ExcelProperty("救援地址")
    private String address;

    /**
     * 救援地址纬度
     */
    @ExcelProperty("救援地址纬度")
    private Double latitude;

    /**
     * 救援地址经度
     */
    @ExcelProperty("救援地址经度")
    private Double longitude;

    /**
     * 订单备注
     */
    @ExcelProperty("订单备注")
    private String remark;

    /**
     * 订单状态：pending-待处理，processing-处理中，completed-已完成，cancelled-已取消
     */
    @ExcelProperty("订单状态")
    private String status;

    /**
     * 状态中文描述（前端展示用）
     */
    private String statusDesc;

    /**
     * 创建时间
     */
    @ExcelProperty("创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty("更新时间")
    private Date updateTime;

    /**
     * 取消时间
     */
    @ExcelProperty("取消时间")
    private Date cancelTime;

    /**
     * 服务商ID
     */
    @ExcelProperty("服务商ID")
    private String supplierId;

    /**
     * 车主ID
     */
    @ExcelProperty("车主ID")
    private Long userId;

    /**
     * 是否评价：0-未评价，1-已评价
     */
    @ExcelProperty("是否评价")
    private Integer evaluateStatus;

    // 救援公司信息
    @ExcelProperty("供应商")
    private String supplierName;
    @ExcelProperty("联系人")
    private String supplierContact;
    @ExcelProperty("联系电话")
    private String supplierPhone;

    // 维修师傅信息
    @ExcelProperty("维修师傅姓名")
    private String staffName;
    @ExcelProperty("维修师傅电话")
    private String staffPhone;
    @ExcelProperty("维修师傅技能")
    private String staffSkill;
}