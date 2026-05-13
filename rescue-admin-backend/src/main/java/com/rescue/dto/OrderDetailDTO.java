package com.rescue.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class OrderDetailDTO {

    // 导出字段
    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("故障类型名称")
    private String faultTypeName;

    @ExcelProperty("车主姓名")
    private String nickname;

    @ExcelProperty("车主电话")
    private String phone;

    @ExcelProperty("救援地址")
    private String address;

    @ExcelProperty("订单备注")
    private String remark;

    @ExcelProperty("订单状态")
    private String statusDesc;

    @ExcelProperty("创建时间")
    private Date createTime;

    @ExcelProperty("更新时间")
    private Date updateTime;

    @ExcelProperty("取消时间")
    private Date cancelTime;

    @ExcelProperty("是否评价")
    private String evaluateStatusDesc; // 纯中文，不参与数据库映射

    @ExcelProperty("评价内容")
    private String content;

    @ExcelProperty("救援公司")
    private String supplierName;

    @ExcelProperty("公司联系人")
    private String supplierContact;

    @ExcelProperty("公司电话")
    private String supplierPhone;

    @ExcelProperty("维修师傅姓名")
    private String staffName;

    @ExcelProperty("维修师傅电话")
    private String staffPhone;


    // ========== 全部忽略，不影响导出 ==========
    @ExcelIgnore
    private Integer faultTypeId;
    @ExcelIgnore
    private Double latitude;
    @ExcelIgnore
    private Double longitude;
    @ExcelIgnore
    private String status;
    @ExcelIgnore
    private String supplierId;
    @ExcelIgnore
    private Long userId;
    @ExcelIgnore
    private Integer evaluateStatus;
    @ExcelIgnore
    private String staffSkill;
}