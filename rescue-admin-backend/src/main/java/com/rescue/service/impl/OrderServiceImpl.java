package com.rescue.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.entity.Order;
import com.rescue.mapper.OrderMapper;
import com.rescue.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final String DEFAULT_STATUS = "pending"; // 默认状态：待处理

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createRescueOrder(Order order) {
        // 1. 生成唯一订单号 (Snowflake ID 或 UUID)
        // 这里使用 Hutool 的雪花算法生成分布式ID
        String orderNo = IdUtil.getSnowflakeNextIdStr();

        // 2. 补全订单实体信息
        order.setOrderNo(orderNo);
        order.setStatus(DEFAULT_STATUS); // 默认为待处理
        order.setEvaluateStatus(0);    // 未评价
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());


        // 3. 存入数据库
        this.save(order);

        return orderNo;
    }


}