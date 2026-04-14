package com.rescue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.entity.*;
import com.rescue.mapper.*;
import com.rescue.service.OwnerOrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 小程序车主端订单专属服务实现类（完全独立，不耦合服务商端逻辑）
 */
@Service
public class OwnerOrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OwnerOrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderEvaluateMapper orderEvaluateMapper;

    @Autowired
    private FaultTypeMapper faultTypeMapper;

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private StaffMapper staffMapper;


    /**
     * 车主分页查询自己的订单列表
     */
    @Override
    public IPage<Order> getOwnerOrderList(Page<Order> page, Long userId, String status, String startDate, String endDate) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId) // 仅查询当前车主订单
                .orderByDesc(Order::getCreateTime);

        // 状态筛选（适配你的状态枚举）
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }

        // 时间范围筛选
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE).atStartOfDay();
            wrapper.ge(Order::getCreateTime, Date.from(start.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE).atTime(23, 59, 59);
            wrapper.le(Order::getCreateTime, Date.from(end.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }

        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 根据订单编号查询订单详情（车主专用）
     */
    @Override
    public Order getOwnerOrderByNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 车主取消订单（仅待处理状态可取消）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOwnerOrder(String orderNo, Long userId) {
        // 1. 查询订单并校验归属
        Order order = this.getOwnerOrderByNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)) {
            return false; // 订单不存在或不属于当前车主
        }

        // 2. 仅待处理状态可取消
        if (!"pending".equals(order.getStatus())) {
            return false; // 非待处理状态，不允许取消
        }

        // 3. 更新订单状态为已取消
        order.setStatus("cancelled");
        order.setUpdateTime(new Date());
        baseMapper.updateById(order);
        return true;
    }

    /**
     * 车主提交订单评价（完全适配你的实体和评价表）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluateOwnerOrder(String orderNo, Long userId, Integer score, String content) {
        // 1. 查询订单并校验归属
        Order order = this.getOwnerOrderByNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权评价该订单");
        }

        // 2. 校验订单状态（仅已完成可评价）
        if (!"completed".equals(order.getStatus())) {
            throw new RuntimeException("仅已完成订单可评价");
        }

        // 3. 校验是否已评价
        if (order.getEvaluateStatus() == 1) {
            throw new RuntimeException("该订单已评价，不可重复评价");
        }

        // 4. 校验服务商ID（你的原有字段）
        String supplierId = order.getSupplierId();
        if (supplierId == null || supplierId.isEmpty()) {
            throw new RuntimeException("订单未关联服务商，无法评价");
        }

        // 5. 校验评分范围（1-5星）
        if (score == null || score < 1 || score > 5) {
            throw new RuntimeException("评分必须为1-5星");
        }

        // 6. 保存评价记录（适配你的OrderEvaluate实体）
        OrderEvaluate evaluate = new OrderEvaluate();
        evaluate.setOrderNo(orderNo);
        evaluate.setUserId(userId);
        evaluate.setSupplierId(supplierId);
        evaluate.setScore(score);
        evaluate.setContent(content == null ? "" : content);
        evaluate.setCreateTime(new Date());
        orderEvaluateMapper.insert(evaluate);

        // 7. 标记订单为已评价
        order.setEvaluateStatus(1);
        order.setUpdateTime(new Date());
        baseMapper.updateById(order);
    }

    /**
     * 统计车主的订单总数（按状态）
     */
    @Override
    public long countOwnerOrders(Long userId, String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId);

        // 按状态筛选
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }

        return baseMapper.selectCount(wrapper);
    }

    /**
     * 车主查询订单详情（关联故障类型、救援公司、维修师傅表）
     */
    @Autowired
    private OrderStaffMapper orderStaffMapper;
    @Override
    public OrderDetailDTO getOwnerOrderDetail(String orderNo, Long userId) {
        // 1. 优先调用Mapper关联查询
        OrderDetailDTO dto = orderMapper.selectOrderDetailByOrderNo(orderNo, userId);

        if (dto == null) {
            // 查询订单基础信息（校验归属）
            Order order = this.getOwnerOrderByNo(orderNo);
            if (order == null || !order.getUserId().equals(userId)) {
                throw new RuntimeException("订单不存在或无权查看");
            }

            dto = new OrderDetailDTO();
            BeanUtils.copyProperties(order, dto);

            // 关联故障类型名称
            if (order.getFaultTypeId() != null) {
                FaultType faultType = faultTypeMapper.selectById(order.getFaultTypeId());
                if (faultType != null) {
                    dto.setFaultTypeName(faultType.getName());
                }
            }

            if (StringUtils.hasText(order.getSupplierId())) {
                Supplier supplier = supplierMapper.selectById(order.getSupplierId());
                if (supplier != null) {
                    dto.setSupplierName(supplier.getName());
                    dto.setSupplierPhone(supplier.getPhone());
                    dto.setSupplierContact(supplier.getContact());
                }


                LambdaQueryWrapper<OrderStaff> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderStaff::getOrderNo, orderNo);
                OrderStaff orderStaff = orderStaffMapper.selectOne(wrapper);

                if (orderStaff != null) {
                    Staff staff = staffMapper.selectById(orderStaff.getStaffId());
                    if (staff != null) {
                        dto.setStaffName(staff.getName());
                        dto.setStaffPhone(staff.getPhone());
                        dto.setStaffSkill(staff.getSkill());
                    }
                }
            }
        }

        dto.setStatusDesc(getStatusDesc(dto.getStatus()));
        return dto;
    }

    /**
     * 车主分页查询订单列表（关联故障类型表）
     */
    @Override
    public IPage<OrderDetailDTO> getOwnerOrderListWithFaultType(Page<Order> page, Long userId, String status, String startDate, String endDate) {
        // 直接调用XML中的关联查询方法
        IPage<OrderDetailDTO> orderPage = orderMapper.selectOrderListWithFaultType(
                page,
                userId,
                status,
                startDate,
                endDate
        );

        // 补充状态中文描述
        for (OrderDetailDTO dto : orderPage.getRecords()) {
            dto.setStatusDesc(getStatusDesc(dto.getStatus()));
        }
        return orderPage;
    }

    /**
     * 获取所有启用的故障类型列表（供前端一键呼救页面选择）
     */
    @Override
    public List<FaultType> getEnableFaultTypeList() {
        LambdaQueryWrapper<FaultType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultType::getStatus, 1) // 只查启用状态
                .orderByAsc(FaultType::getSort); // 按排序字段升序
        return faultTypeMapper.selectList(wrapper);
    }

    // 辅助方法：转换订单状态为中文描述
    private String getStatusDesc(String status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status) {
            case "pending":
                return "待处理";
            case "processing":
                return "处理中";
            case "completed":
                return "已完成";
            case "cancelled":
                return "已取消";
            default:
                return "未知状态";
        }
    }
}