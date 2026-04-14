package com.rescue.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.entity.Order;

/**
 * 订单核心服务接口
 * 包含：车主创建订单、服务商/车主查询订单列表等核心逻辑
 */
public interface OrderService extends IService<Order> {

    /**
     * 车主发起救援订单
     * @param order 订单参数（包含故障类型、救援地址、联系人等）
     * @return 生成的唯一订单号
     */
    String createRescueOrder(Order order);



}