package com.rescue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单Mapper接口（关联t_fault_type查询故障类型名称）
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    // ==================== 服务商工作台统计相关 ====================
    Order selectByOrderNo(String orderNo);
    int countPendingOrdersBySupplier(String supplierId);
    int countTodayNewOrdersBySupplier(String supplierId);
    int countCompletedOrdersBySupplier(String supplierId);
    int countMonthCompletedOrdersBySupplier(String supplierId);

    // ==================== 车主端订单列表查询（关联故障类型） ====================
    /**
     * 车主分页查询订单列表（关联故障类型表，返回故障类型名称）
     * @param page 分页参数（MyBatis-Plus分页对象，必须放在第一个）
     * @param userId 车主ID
     * @param status 订单状态（可选）
     * @param startDate 开始时间（可选）
     * @param endDate 结束时间（可选）
     * @return 分页的订单详情DTO（包含faultTypeName）
     */
    IPage<OrderDetailDTO> selectOrderListWithFaultType(
            Page<Order> page,  // MyBatis-Plus分页参数（必须第一个）
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    /**
     * 车主查询订单详情（关联故障类型、救援公司、维修师傅）
     * @param orderNo 订单编号
     * @param userId 车主ID（用于校验归属）
     * @return 订单详情DTO（包含faultTypeName）
     */
    OrderDetailDTO selectOrderDetailByOrderNo(
            @Param("orderNo") String orderNo,
            @Param("userId") Long userId);

    // ==================== 服务商端订单列表查询（核心修复） ====================
    /**
     * 服务商分页查询订单列表（关联故障类型表，返回故障类型名称）
     * @param page 分页参数（MyBatis-Plus分页对象）
     * @param supplierId 服务商ID
     * @return 分页的订单详情DTO（包含faultTypeName）
     */
    IPage<OrderDetailDTO> selectOrderListBySupplier(
            Page<Order> page,  // 分页参数（必须第一个）
            @Param("supplierId") String supplierId);

    //IPage<OrderDetailDTO> getSupplierOrderListWithFullDetail(Page<OrderDetailDTO> page, String supplierId);

    IPage<OrderDetailDTO> selectSupplierOrderListWithFullDetail(Page<OrderDetailDTO> page, @Param("supplierId") String supplierId);

    // 管理员查询所有订单（完整联表）
    IPage<OrderDetailDTO> selectAdminOrderList(Page<OrderDetailDTO> page,
                                               @Param("orderNo") String orderNo,
                                               @Param("status") String status,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);
}