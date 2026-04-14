package com.rescue.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.entity.FaultType;
import com.rescue.entity.Order;

import java.util.List;

/**
 * 车主端订单专属服务接口（完全独立于服务商端）
 */
public interface OwnerOrderService extends IService<Order> {



    /**
     * 车主分页查询自己的订单列表
     * @param page 分页参数
     * @param userId 车主ID
     * @param status 订单状态（可选：pending/processing/completed/cancelled）
     * @param startDate 开始时间（可选）
     * @param endDate 结束时间（可选）
     * @return 分页订单列表
     */
    IPage<Order> getOwnerOrderList(Page<Order> page, Long userId, String status, String startDate, String endDate);

    /**
     * 根据订单编号查询订单详情（车主专用）
     * @param orderNo 订单编号
     * @return 订单详情
     */
    Order getOwnerOrderByNo(String orderNo);

    /**
     * 车主取消订单（仅待处理状态可取消）
     * @param orderNo 订单编号
     * @param userId 车主ID（校验订单归属）
     * @return 是否取消成功
     */
    boolean cancelOwnerOrder(String orderNo, Long userId);

    /**
     * 车主提交订单评价
     * @param orderNo 订单编号
     * @param userId 车主ID（校验订单归属）
     * @param score 评分（1-5星）
     * @param content 评价内容
     */
    void evaluateOwnerOrder(String orderNo, Long userId, Integer score, String content);

    /**
     * 统计车主的订单总数（按状态）
     * @param userId 车主ID
     * @param status 订单状态（可选）
     * @return 订单数量
     */
    long countOwnerOrders(Long userId, String status);

    /**
     * 获取所有启用的故障类型列表（供前端选择）
     */
    List<FaultType> getEnableFaultTypeList();

    /**
     * 车主分页查询订单列表（关联故障类型表）
     */
    IPage<OrderDetailDTO> getOwnerOrderListWithFaultType(Page<Order> page, Long userId, String status, String startDate, String endDate);

    /**
     * 车主查询订单详情（关联故障类型、救援公司、维修师傅表）
     */
    OrderDetailDTO getOwnerOrderDetail(String orderNo, Long userId);
}