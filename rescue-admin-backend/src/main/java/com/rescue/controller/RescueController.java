package com.rescue.controller;

import com.rescue.entity.Order;
import com.rescue.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rescue")
public class RescueController {

    @Autowired
    private OrderService orderService;

    /**
     * 前端对应接口：/rescue/create
     * 接收小程序传来的订单信息，存入数据库
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Order order) {
        // 调用服务层创建订单
        String orderNo = orderService.createRescueOrder(order);

        // 构建返回结果（对应前端 res.data.orderNo）
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);

        return ResponseEntity.ok(result);
    }
}