package com.rescue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    List<OrderDetailDTO> selectOrderList(@Param("supplierId") String supplierId);

    IPage<OrderDetailDTO> selectOrderPage(
            Page<OrderDetailDTO> page,
            @Param("supplierId") String supplierId,
            @Param("orderNo") String orderNo,
            @Param("status") String status,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    OrderDetailDTO selectOrderDetail(@Param("orderNo") String orderNo);

    int acceptOrder(@Param("orderNo") String orderNo, @Param("supplierId") String supplierId);

    int completeOrder(@Param("orderNo") String orderNo);

    // ✅ 方法名完全和 XML 一致
    List<OrderDetailDTO> listLatestPendingSimpleOrders(
            @Param("supplierId") String supplierId,
            @Param("limit") int limit
    );

    List<Map<String, Object>> countFaultTypeDistribution(@Param("supplierId") String supplierId);

    List<Map<String, Object>> countLast7DaysOrder(@Param("supplierId") String supplierId);
}