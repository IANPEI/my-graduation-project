package com.rescue.service.impl;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.dto.OrderStatisticsDTO;
import com.rescue.dto.StaffDTO;
import com.rescue.entity.*;
import com.rescue.mapper.*;
import com.rescue.service.SupplierOrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.excel.EasyExcel;

@Service
public class SupplierOrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements SupplierOrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private FaultTypeMapper faultTypeMapper;
    @Autowired
    private SupplierMapper supplierMapper;



    @Override
    public void exportOrder(String orderNo, HttpServletResponse response) {
        try {
            // 1. 查询（SQL 已确认完全正常）
            OrderDetailDTO orderDetail = supplierMapper.selectOrderDetail(orderNo);

            // 2. 状态中文
            orderDetail.setStatusDesc(getStatusDesc(orderDetail.getStatus()));

            // 3. 评价状态中文
            String evaluateText = (orderDetail.getEvaluateStatus() != null && orderDetail.getEvaluateStatus() == 1)
                    ? "已评价"
                    : "未评价";
            orderDetail.setEvaluateStatusDesc(evaluateText);

            // 4. Excel 响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("订单_" + orderNo, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 5. 最简单导出，不搞任何复杂配置
            List<OrderDetailDTO> list = new ArrayList<>();
            list.add(orderDetail);

            // 最稳定写法！！！
            try (OutputStream out = response.getOutputStream()) {
                EasyExcel.write(out, OrderDetailDTO.class)
                        .sheet("订单详情")
                        .doWrite(list);
            }

        } catch (Exception e) {
            // 必须打印真实错误！！！
            e.printStackTrace();
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Override
    public IPage<OrderDetailDTO> getSupplierOrderListWithFullDetail(Page<OrderDetailDTO> page, String supplierId) {
        // 1. 优先调用Mapper关联查询（批量分页查询，推荐方式）
        IPage<OrderDetailDTO> dtoPage = orderMapper.selectSupplierOrderListWithFullDetail(page, supplierId);

        // 2. 兼容方案：遍历每条数据，对关联查询为空的字段手动补充（对齐车主端兜底逻辑）
        if (dtoPage.getRecords() != null && !dtoPage.getRecords().isEmpty()) {
            for (OrderDetailDTO dto : dtoPage.getRecords()) {
                // 如果SQL关联查询未获取到完整信息，手动补充
                if (dto.getFaultTypeName() == null) {
                    // 关联故障类型名称
                    if (dto.getFaultTypeId() != null) {
                        FaultType faultType = faultTypeMapper.selectById(dto.getFaultTypeId());
                        if (faultType != null) {
                            dto.setFaultTypeName(faultType.getName());
                        }
                    }
                }

                if (dto.getSupplierName() == null && dto.getSupplierId() != null && !dto.getSupplierId().isEmpty()) {
                    // 关联救援公司信息
                    Supplier supplier = supplierMapper.selectById(dto.getSupplierId());
                    if (supplier != null) {
                        dto.setSupplierName(supplier.getName());
                        dto.setSupplierPhone(supplier.getPhone());
                        dto.setSupplierContact(supplier.getContact());

                        // 关联维修师傅信息（取该服务商下第一个师傅，对齐车主端逻辑）
                        LambdaQueryWrapper<Staff> staffWrapper = new LambdaQueryWrapper<>();
                        staffWrapper.eq(Staff::getSupplierId, supplier.getId()); // 修正：原代码是Staff::getId，应为supplierId
                        List<Staff> staffList = staffMapper.selectList(staffWrapper);
                        if (!staffList.isEmpty()) {
                            Staff staff = staffList.get(0);
                            dto.setStaffName(staff.getName());
                            dto.setStaffPhone(staff.getPhone());
                            dto.setStaffSkill(staff.getSkill());
                        }
                    }
                }

                // 补充状态中文描述（前端展示友好，对齐车主端逻辑）
                dto.setStatusDesc(getStatusDesc(dto.getStatus()));
            }
        }

        return dtoPage;
    }

    // 状态中文映射方法
    private String getStatusDesc(String status) {
        if (status == null) return "未知状态";
        switch (status) {
            case "pending": return "待救援";
            case "accepted": return "已接单";
            case "processing": return "救援中";
            case "completed": return "已完成";
            case "cancelled": return "已取消";
            default: return "未知状态";
        }
    }


    // 2. 订单列表查询（接收supplierId参数）
    @Override
    public IPage<OrderDetailDTO> getOrderList(
            Integer pageNum,
            Integer pageSize,
            String supplierId,
            String orderNo,
            String status,
            String startDate,
            String endDate) {

        Page<OrderDetailDTO> page = new Page<>(pageNum, pageSize);

        return supplierMapper.selectOrderPage(
                page,
                supplierId,
                orderNo,
                status,
                startDate,
                endDate
        );
    }

    // 详情
    @Override
    public OrderDetailDTO getOrderDetail(String orderNo) {
        return supplierMapper.selectOrderDetail(orderNo);
    }

    // 3. 接单（接收supplierId参数）

    @Autowired
    private OrderStaffMapper orderStaffMapper;
    @Override
    @Transactional
    public void acceptOrder(String orderNo, String supplierId, String staffId) {
        Order order = orderMapper.selectById(orderNo);
        if (order == null || !"pending".equals(order.getStatus())) {
            throw new RuntimeException("订单无法接单");
        }

        // 乐观锁更新：只有状态是 pending 才能更新
        int rows = orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getStatus, "pending")
                        .set(Order::getStatus, "processing")
                        .set(Order::getSupplierId, supplierId)
                        .set(Order::getUpdateTime, new Date())
        );

        if (rows == 0) {
            throw new RuntimeException("订单已被抢走！");
        }

        // 数据库唯一索引兜底
        try {
            OrderStaff os = new OrderStaff();
            os.setOrderNo(orderNo);
            os.setStaffId(staffId);
            os.setSupplierId(supplierId);
            orderStaffMapper.insert(os);
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("订单已被接单！");
        }

        staffMapper.update(null,
                new LambdaUpdateWrapper<Staff>()
                        .eq(Staff::getId, staffId)
                        .set(Staff::getStatus, "busy") // busy=救援中/忙碌
        );
    }

    @Override
    public void cancelOrderBySupplier(String orderNo, String supplierId) {
        // 1. 查询订单是否存在 + 是否属于当前服务商
        Order order = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getSupplierId, supplierId));

        if (order == null) {
            throw new RuntimeException("订单不存在或无权操作");
        }

        // 2. 只能取消【已接单/processing】状态
        if (!"processing".equals(order.getStatus())) {
            throw new RuntimeException("只能取消已接单状态的订单");
        }

        // 3. 修改状态为已取消
        order.setStatus("cancelled");
        updateById(order);
    }

    // 4. 完成订单（接收supplierId参数）
    @Override
    public void completeOrder(String orderNo) {
        supplierMapper.completeOrder(orderNo);
    }

    // 5. 最新待处理订单（接收supplierId参数）
    @Override
    public List<Order> getLatestPendingOrders(String supplierId) {
        if (!StringUtils.hasText(supplierId)) {
            throw new RuntimeException("服务商ID不能为空");
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "pending")
                .eq(Order::getSupplierId, supplierId) // 替换硬编码ID
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT 10");
        return orderMapper.selectList(wrapper);
    }

    // 6. 救援人员列表（接收supplierId参数）
    @Override
    public IPage<Staff> getStaffList(Page<Staff> page, String supplierId, String name) {
        if (!StringUtils.hasText(supplierId)) {
            throw new RuntimeException("服务商ID不能为空");
        }

        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Staff::getSupplierId, supplierId); // 替换硬编码ID
        if (name != null && !name.isEmpty()) wrapper.like(Staff::getName, name);
        wrapper.orderByAsc(Staff::getId);
        return staffMapper.selectPage(page, wrapper);
    }

    // 7. 切换人员状态（接收supplierId参数）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStaffStatus(String supplierId, String id, String status) {
        if (!StringUtils.hasText(supplierId) || !StringUtils.hasText(id) || !StringUtils.hasText(status)) {
            throw new RuntimeException("服务商ID/人员ID/状态不能为空");
        }

        Staff staff = staffMapper.selectById(id);
        if (staff == null) throw new RuntimeException("救援人员不存在");
        // 校验人员归属
        if (!supplierId.equals(staff.getSupplierId())) throw new RuntimeException("无权操作其他服务商人员");
        if (!"online".equals(status) && !"offline".equals(status)) throw new RuntimeException("状态仅支持online/offline");

        staff.setStatus(status);
        staff.setUpdateTime(new Date());
        staffMapper.updateById(staff);
    }

    // 8. 删除救援人员（新增：接收supplierId参数，校验归属）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteStaff(String supplierId, String id) {
        if (!StringUtils.hasText(supplierId) || !StringUtils.hasText(id)) {
            throw new RuntimeException("服务商ID/人员ID不能为空");
        }

        // 先校验人员归属
        Staff staff = staffMapper.selectById(id);
        if (staff == null) {
            return false;
        }
        if (!supplierId.equals(staff.getSupplierId())) {
            return false;
        }

        // 归属校验通过，执行删除
        return staffMapper.deleteById(id) > 0;
    }

    /**
     * 新增救援人员
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStaff(String supplierId, StaffDTO staffDTO) {

        if (!StringUtils.hasText(supplierId)) {
            throw new RuntimeException("服务商ID不能为空"); // 这个异常会被 Controller 捕获并返回 Result.error
        }
        // 校验手机号是否已存在
        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Staff::getSupplierId, supplierId)
                .eq(Staff::getPhone, staffDTO.getPhone());
        if (staffMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("该手机号已绑定救援人员");
        }

        // DTO转实体
        Staff staff = new Staff();
        BeanUtils.copyProperties(staffDTO, staff);
        staff.setSupplierId(supplierId);
        staff.setStatus("online"); // 默认在线
        staff.setCreateTime(new Date());
        staff.setUpdateTime(new Date());

        // 插入数据库
        staffMapper.insert(staff);
    }

    /**
     * 编辑救援人员
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editStaff(String supplierId, StaffDTO staffDTO) {
        // 1. 校验ID：编辑时必须传入有效的 Integer 类型ID
        if (staffDTO.getId() == null || staffDTO.getId() <= 0) {
            throw new RuntimeException("人员ID不能为空");
        }

        // 2. 校验人员归属
        Staff existStaff = staffMapper.selectById(staffDTO.getId());
        if (existStaff == null) {
            throw new RuntimeException("救援人员不存在");
        }

        // 3. 校验是否是当前服务商的人员（防止越权修改）
        if (!supplierId.equals(existStaff.getSupplierId())) {
            throw new RuntimeException("无权限编辑其他服务商的人员");
        }

        // 4. 校验手机号是否已被其他人员占用（排除当前编辑的ID）
        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Staff::getSupplierId, supplierId)
                .eq(Staff::getPhone, staffDTO.getPhone())
                .ne(Staff::getId, staffDTO.getId()); // 排除自己
        if (staffMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("该手机号已绑定其他救援人员");
        }

        // 5. 更新要修改的字段
        existStaff.setName(staffDTO.getName());
        existStaff.setPhone(staffDTO.getPhone());
        existStaff.setSkill(staffDTO.getSkill());
        existStaff.setUpdateTime(new Date()); // 更新时间

        // 6. 执行更新
        staffMapper.updateById(existStaff);
    }


    // 统计待处理订单数
    @Override
    public long countPendingOrders(String supplierId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        // 待处理状态：pending（和你XML里的状态保持一致）
        queryWrapper.eq(Order::getStatus, "pending")
                // 未分配服务商（或空）
                .and(wrapper -> wrapper.isNull(Order::getSupplierId)
                        .or().eq(Order::getSupplierId, "")
                        .or().eq(Order::getSupplierId, supplierId));
        return orderMapper.selectCount(queryWrapper);
    }

    // 查询最新待处理订单
    @Override
    public List<OrderDetailDTO> listLatestPendingOrders(String supplierId, int limit) {
        return supplierMapper.listLatestPendingSimpleOrders(supplierId, limit);
    }


    /**
     * 获取服务商订单统计数据
     */
    @Override
    public OrderStatisticsDTO getOrderStatistics(String supplierId) {
        OrderStatisticsDTO dto = new OrderStatisticsDTO();

        // 1. 统计故障类型分布
        List<Map<String, Object>> faultTypeList = supplierMapper.countFaultTypeDistribution(supplierId);
        Map<String, Integer> faultTypeMap = faultTypeList.stream()
                .collect(Collectors.toMap(
                        item -> (String) item.get("faultTypeName"),
                        item -> Integer.parseInt(item.get("count").toString()),
                        (k1, k2) -> k2 // 去重取后者
                ));
        dto.setFaultTypeDistribution(faultTypeMap);

        // 2. 统计近7日订单数量（补全无订单的日期为0）
        List<Map<String, Object>> dayCountList = supplierMapper.countLast7DaysOrder(supplierId);
        // 先构建近7天的日期（格式：MM-DD）
        Map<String, Integer> dayCountMap = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String date = String.format("%02d-%02d", cal.get(Calendar.MONTH)+1, cal.get(Calendar.DATE));
            dayCountMap.put(date, 0); // 初始化所有日期为0
        }
        // 填充有订单的日期数量
        for (Map<String, Object> item : dayCountList) {
            String date = (String) item.get("orderDate");
            Integer count = Integer.parseInt(item.get("count").toString());
            dayCountMap.put(date, count);
        }
        // 转换为前端需要的List格式
        List<Map<String, Object>> last7DaysList = dayCountMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // 按日期排序
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .collect(Collectors.toList());
        dto.setLast7DaysOrderCount(last7DaysList);

        return dto;
    }

    @Override
    public IPage<OrderDetailDTO> getAdminOrderList(
            Integer pageNum,
            Integer pageSize,
            String orderNo,
            String status,
            String startDate,
            String endDate) {

        Page<OrderDetailDTO> page = new Page<>(pageNum, pageSize);

        // 调用你XML里的管理员全量查询
        IPage<OrderDetailDTO> dtoPage = orderMapper.selectAdminOrderList(
                page, orderNo, status, startDate, endDate);

        // 填充中文状态
        dtoPage.getRecords().forEach(dto -> {
            dto.setStatusDesc(getStatusDesc(dto.getStatus()));
            dto.setEvaluateStatusDesc(convertEvaluateDesc(dto.getEvaluateStatus()));
        });

        return dtoPage;
    }
    // 评价状态中文
    private String convertEvaluateDesc(Integer evaluateStatus) {
        if (evaluateStatus == null) return "未评价";
        return evaluateStatus == 1 ? "已评价" : "未评价";
    }
}