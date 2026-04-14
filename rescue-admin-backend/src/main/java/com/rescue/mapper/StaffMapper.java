package com.rescue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rescue.entity.Staff;
import org.apache.ibatis.annotations.Mapper;

/**
 * 救援人员Mapper
 */
@Mapper
public interface StaffMapper extends BaseMapper<Staff> {
}