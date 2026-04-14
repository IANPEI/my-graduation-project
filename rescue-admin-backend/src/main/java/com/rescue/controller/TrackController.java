package com.rescue.controller;

import com.rescue.service.TrackService;
import com.rescue.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 实时位置追踪模块（服务商上传 + 车主获取）
 */
@RestController
@RequestMapping("/track")
public class TrackController {

    @Resource
    private TrackService trackService;

    /**
     * 服务商上传实时位置
     */
    @PostMapping("/upload")
    public Result<?> uploadLocation(
            @RequestParam String orderNo,
            @RequestParam Double supplierLat,
            @RequestParam Double supplierLng) {
        return trackService.uploadSupplierLocation(orderNo, supplierLat, supplierLng);
    }

    /**
     * 车主获取服务商实时位置
     */
    @GetMapping("/get")
    public Result<Map<String, Double>> getLocation(@RequestParam String orderNo) {
        return trackService.getSupplierLocation(orderNo);
    }
}