package com.rescue.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_order_staff")
public class OrderStaff {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String staffId;
    private String supplierId;
    private Date createTime;
}
