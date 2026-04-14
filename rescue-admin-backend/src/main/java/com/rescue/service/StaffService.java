package com.rescue.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rescue.dto.StaffDTO;
import com.rescue.entity.Staff;

/**
 * 救援人员服务接口
 */
public interface StaffService extends IService<Staff> {
    /**
     * 获取人员列表
     */
    IPage<Staff> getStaffList(Page<Staff> page, String name);

    /**
     * 切换人员状态
     */
    void changeStatus(Integer id, String status);


}