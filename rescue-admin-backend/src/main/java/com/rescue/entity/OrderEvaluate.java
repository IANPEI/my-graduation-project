package com.rescue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_order_evaluate")
public class OrderEvaluate {
    @TableId(type = IdType.AUTO)
    private Long id;          // 评价ID (主键，自增)
    private String orderNo;   // 订单编号
    private Long userId;      // 评价车主ID
    private String supplierId;// 被评价服务商ID
    private Integer score;    // 评分 (1-5星)
    private String content;   // 评价内容
    private Date createTime;  // 评价时间
}