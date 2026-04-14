package com.rescue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rescue.entity.FaultType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 故障类型Mapper
 */
@Mapper
public interface FaultTypeMapper extends BaseMapper<FaultType> {
}