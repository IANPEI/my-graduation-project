package com.rescue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rescue.entity.OrderEvaluate;

public interface OrderEvaluateService extends IService<OrderEvaluate> {
    OrderEvaluate getByOrderNo(String orderNo);
}