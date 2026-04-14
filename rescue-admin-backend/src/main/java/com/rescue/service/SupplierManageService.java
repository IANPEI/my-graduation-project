package com.rescue.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rescue.entity.Supplier;

import java.util.List;
import java.util.Map;

/**
 * 管理员端-服务商管理专属服务
 */
public interface SupplierManageService extends IService<Supplier> {
    Map<String, Object> getAdminHomeData();
    IPage<Supplier> getSupplierList(Page<Supplier> page, String name, String status);
    void changeSupplierStatus(String id, String status);
    List<Supplier> getLatestApplyList();

    /**
     * 根据ID查询服务商详情
     */
    Supplier getSupplierById(String id);

    boolean checkSupplierIdUnique(String id);

    /**
     * 新增服务商
     */
    boolean saveSupplier(Supplier supplier);

    /**
     * 编辑服务商
     */
    boolean updateSupplier(Supplier supplier);


    void auditSupplier(String supplierId, String status);

}