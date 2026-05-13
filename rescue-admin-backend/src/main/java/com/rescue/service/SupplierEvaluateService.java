package com.rescue.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rescue.entity.Supplier;
import com.rescue.dto.SupplierEvaluateVO;

public interface SupplierEvaluateService extends IService<Supplier> {
    IPage<SupplierEvaluateVO> getSupplierEvaluatePage(Integer pageNum, Integer pageSize);
}
