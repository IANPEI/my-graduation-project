package com.rescue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.entity.Supplier;
import com.rescue.mapper.OrderEvaluateMapper;
import com.rescue.mapper.SupplierMapper;
import com.rescue.dto.SupplierEvaluateVO;
import com.rescue.service.SupplierEvaluateService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class SupplierEvaluateServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierEvaluateService {

    @Resource
    private OrderEvaluateMapper orderEvaluateMapper;

    @Override
    public IPage<SupplierEvaluateVO> getSupplierEvaluatePage(Integer pageNum, Integer pageSize) {
        // 1. 分页查询已启用的服务商
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getStatus, "enable");

        IPage<Supplier> supplierPage = this.page(new Page<>(pageNum, pageSize), wrapper);

        // 2. 封装 VO
        IPage<SupplierEvaluateVO> voPage = new Page<>(pageNum, pageSize);
        List<SupplierEvaluateVO> list = new ArrayList<>();

        for (Supplier supplier : supplierPage.getRecords()) {
            SupplierEvaluateVO vo = new SupplierEvaluateVO();
            BeanUtils.copyProperties(supplier, vo);
            vo.setSupplierId(supplier.getId()); // 修复 ID 为 null 问题

            // 统计
            Integer total = orderEvaluateMapper.getTotalEvaluateCount(supplier.getId());
            Integer good = orderEvaluateMapper.getGoodEvaluateCount(supplier.getId());
            BigDecimal avg = orderEvaluateMapper.getAvgScore(supplier.getId());

            total = total == null ? 0 : total;
            good = good == null ? 0 : good;

            BigDecimal goodRate = BigDecimal.ZERO;
            if (total > 0) {
                goodRate = new BigDecimal(good)
                        .divide(new BigDecimal(total), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }

            vo.setSupplierId(supplier.getId());
            vo.setTotalCount(total);
            vo.setGoodCount(good);
            vo.setGoodRate(goodRate);
            vo.setAvgScore(avg);

            list.add(vo);
        }

        voPage.setRecords(list);
        voPage.setTotal(supplierPage.getTotal());
        return voPage;
    }
}
