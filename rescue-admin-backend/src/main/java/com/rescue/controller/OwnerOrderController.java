package com.rescue.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.entity.FaultType;
import com.rescue.entity.Order;
import com.rescue.service.OwnerOrderService;
import com.rescue.service.SupplierOrderService;
import com.rescue.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 车主端订单控制器（关联故障类型查询）
 */
@RestController
@RequestMapping("/owner/order")
public class OwnerOrderController {

    @Autowired
    private OwnerOrderService ownerOrderService;

    /**
     * 获取启用的故障类型列表（一键呼救页面选择）
     */
    @GetMapping("/fault/type/list")
    public Result<List<FaultType>> getEnableFaultTypeList() {
        return Result.success(ownerOrderService.getEnableFaultTypeList());
    }

    /**
     * 车主查询订单详情（含故障类型、救援公司、维修师傅）
     */
    @GetMapping("/detail/{orderNo}")
    public Result<OrderDetailDTO> getOrderDetail(
            @PathVariable String orderNo,
            @RequestParam Long userId) {
        return Result.success(ownerOrderService.getOwnerOrderDetail(orderNo, userId));
    }

    /**
     * 车主分页查询订单列表（含故障类型名称）
     */
    @GetMapping("/list")
    public Result<IPage<OrderDetailDTO>> getOrderList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        IPage<OrderDetailDTO> orderPage = ownerOrderService.getOwnerOrderListWithFaultType(page, userId, status, startDate, endDate);
        return Result.success(orderPage);
    }


    /**
     * 车主取消订单
     */
    @PostMapping("/cancel")
    public Result<Boolean> cancelOrder(
            @RequestParam String orderNo,
            @RequestParam Long userId) {
        boolean success = ownerOrderService.cancelOwnerOrder(orderNo, userId);
        return success ? Result.success(true) : Result.error("取消失败：订单非待处理状态或不属于当前用户");
    }

    /**
     * 车主提交订单评价
     */
    @PostMapping("/evaluate")
    public Result<Void> evaluateOrder(
            @RequestParam String orderNo,
            @RequestParam Long userId,
            @RequestParam Integer score,
            @RequestParam String content) {
        ownerOrderService.evaluateOwnerOrder(orderNo, userId, score, content);
        return Result.success();
    }

    @Autowired
    private SupplierOrderService supplierOrderService;
    @GetMapping("/export/{orderNo}")
    public void exportOrder(
            @PathVariable String orderNo,
            @RequestParam Long userId,
            HttpServletResponse response) {

        supplierOrderService.exportOrder(orderNo, response);
    }
}