package com.rescue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.entity.Supplier;

import java.util.List;
import java.util.Map;

/**
 * 服务商Service
 */
public interface SupplierService extends IService<Supplier> {

    /**
     * 根据服务商ID查询资料
     * @param supplierId 服务商ID
     * @return 服务商资料Map
     */
    Map<String, Object> getSupplierProfileById(String supplierId);

    /**
     * 更新服务商资料
     * @param supplierId 服务商ID
     * @param params 更新参数（name/contact/phone/city/address等）
     */
    void updateSupplierProfile(String supplierId, Map<String, String> params);

    /**
     * 统计待处理订单数
     * @param supplierId 服务商ID
     * @return 待处理订单数量
     */
    long countPendingOrders(String supplierId);

    /**
     * 查询最新的待处理订单（前N条）
     * @param supplierId 服务商ID
     * @param limit 条数限制
     * @return 待处理订单列表
     */
    List<OrderDetailDTO> listLatestPendingOrders(String supplierId, int limit);
}