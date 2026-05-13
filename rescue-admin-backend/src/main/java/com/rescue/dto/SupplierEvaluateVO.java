package com.rescue.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SupplierEvaluateVO {
    private String supplierId;      // 商家ID
    private String name;           // 商家名称
    private String contact;        // 联系人
    private String phone;          // 电话
    private String city;           // 城市
    private Integer totalCount;    // 总评价数
    private Integer goodCount;     // 好评数
    private BigDecimal goodRate;   // 好评率（%）
    private BigDecimal avgScore;   // 平均评分
}