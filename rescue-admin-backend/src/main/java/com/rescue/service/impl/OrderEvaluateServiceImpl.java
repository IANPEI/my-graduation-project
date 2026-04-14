package com.rescue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.entity.OrderEvaluate;
import com.rescue.mapper.OrderEvaluateMapper;
import com.rescue.service.OrderEvaluateService;
import org.springframework.stereotype.Service;

@Service
public class OrderEvaluateServiceImpl extends ServiceImpl<OrderEvaluateMapper, OrderEvaluate> implements OrderEvaluateService {

    @Override
    public OrderEvaluate getByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderEvaluate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderEvaluate::getOrderNo, orderNo);
        return getOne(wrapper);
    }
}