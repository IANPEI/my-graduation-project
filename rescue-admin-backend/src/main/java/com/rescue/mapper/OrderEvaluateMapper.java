package com.rescue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rescue.entity.OrderEvaluate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface OrderEvaluateMapper extends BaseMapper<OrderEvaluate> {
    // 查询某个商家总评价数
    Integer getTotalEvaluateCount(@Param("supplierId") String supplierId);

    // 查询某个商家好评数（4星+5星）
    Integer getGoodEvaluateCount(@Param("supplierId") String supplierId);

    // 查询某个商家平均分
    BigDecimal getAvgScore(@Param("supplierId") String supplierId);
}