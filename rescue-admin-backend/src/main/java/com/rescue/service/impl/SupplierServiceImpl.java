package com.rescue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.entity.Order;
import com.rescue.entity.Supplier;
import com.rescue.mapper.OrderMapper;
import com.rescue.mapper.SupplierMapper;
import com.rescue.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    @Autowired
    private SupplierMapper supplierMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public Map<String, Object> getSupplierProfileById(String supplierId) {
        Supplier supplier = this.getById(supplierId);
        if (supplier == null) {
            throw new RuntimeException("服务商不存在");
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", supplier.getId());
        profile.put("name", supplier.getName());
        profile.put("contact", supplier.getContact());
        profile.put("phone", supplier.getPhone());
        profile.put("city", supplier.getCity());
        profile.put("address", supplier.getAddress());
        profile.put("status", supplier.getStatus());
        profile.put("createTime", supplier.getCreateTime());
        profile.put("updateTime", supplier.getUpdateTime());

        return profile;
    }

    @Override
    public void updateSupplierProfile(String supplierId, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new RuntimeException("更新参数不能为空");
        }

        LambdaUpdateWrapper<Supplier> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Supplier::getId, supplierId);

        if (StringUtils.hasText(params.get("name"))) {
            updateWrapper.set(Supplier::getName, params.get("name"));
        }
        if (StringUtils.hasText(params.get("contact"))) {
            updateWrapper.set(Supplier::getContact, params.get("contact"));
        }
        if (StringUtils.hasText(params.get("phone"))) {
            updateWrapper.set(Supplier::getPhone, params.get("phone"));
        }
        if (StringUtils.hasText(params.get("city"))) {
            updateWrapper.set(Supplier::getCity, params.get("city"));
        }
        if (StringUtils.hasText(params.get("address"))) {
            updateWrapper.set(Supplier::getAddress, params.get("address"));
        }

        updateWrapper.set(Supplier::getUpdateTime, new Date());
        boolean success = this.update(updateWrapper);
        if (!success) {
            throw new RuntimeException("服务商资料更新失败");
        }
    }

    @Override
    public long countPendingOrders(String supplierId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getSupplierId, supplierId)
                .eq(Order::getStatus, "pending");
        return orderMapper.selectCount(queryWrapper);
    }

    // ====================== 这里改成调用简单版 ======================
    @Override
    public List<OrderDetailDTO> listLatestPendingOrders(String supplierId, int limit) {
        // 调用【不会报错的简单SQL】
        return supplierMapper.listLatestPendingSimpleOrders(supplierId, limit);
    }

}