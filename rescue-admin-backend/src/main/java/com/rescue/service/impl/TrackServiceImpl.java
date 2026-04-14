package com.rescue.service.impl;

import com.rescue.entity.Order;
import com.rescue.mapper.OrderMapper;
import com.rescue.service.TrackService;
import com.rescue.util.Result;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class TrackServiceImpl implements TrackService {

    @Resource
    private OrderMapper orderMapper;

    /**
     * 上传服务商位置（直接更新订单表）
     */
    @Override
    public Result<?> uploadSupplierLocation(String orderNo, Double supplierLat, Double supplierLng) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setSupplierLat(supplierLat);
        order.setSupplierLng(supplierLng);
        int rows = orderMapper.updateById(order);

        if (rows > 0) {
            return Result.success("位置上传成功");
        } else {
            return Result.error("订单不存在，上传失败");
        }
    }

    /**
     * 获取服务商实时位置
     */
    @Override
    public Result<Map<String, Double>> getSupplierLocation(String orderNo) {
        Order order = orderMapper.selectById(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getSupplierLat() == null || order.getSupplierLng() == null) {
            return Result.error("服务商尚未出发，暂无位置");
        }

        Map<String, Double> map = new HashMap<>();
        map.put("lat", order.getSupplierLat());
        map.put("lng", order.getSupplierLng());
        return Result.success(map);
    }
}