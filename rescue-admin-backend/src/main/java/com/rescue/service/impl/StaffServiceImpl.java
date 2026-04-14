package com.rescue.service.impl;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.dto.StaffDTO;
import com.rescue.entity.Staff;
import com.rescue.mapper.StaffMapper;
import com.rescue.service.StaffService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;

/**
 * 救援人员服务实现类
 */
@Service
public class StaffServiceImpl extends ServiceImpl<StaffMapper, Staff> implements StaffService {

    /**
     * 获取人员列表
     */
    @Override
    public IPage<Staff> getStaffList(Page<Staff> page, String name) {
        LambdaQueryWrapper<Staff> wrapper = new LambdaQueryWrapper<>();
        // 姓名模糊查询
        if (name != null && !name.isEmpty()) {
            wrapper.like(Staff::getName, name);
        }
        // 按创建时间降序
        wrapper.orderByDesc(Staff::getCreateTime);
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 切换人员状态
     */
    @Override
    public void changeStatus(Integer id, String status) {
        Staff staff = new Staff();
        staff.setId(id);
        staff.setStatus(status);
        baseMapper.updateById(staff);
    }



}