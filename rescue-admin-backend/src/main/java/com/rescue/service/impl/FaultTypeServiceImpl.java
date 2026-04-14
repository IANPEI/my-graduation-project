package com.rescue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.entity.FaultType;
import com.rescue.mapper.FaultTypeMapper;
import com.rescue.service.FaultTypeService;
import org.springframework.stereotype.Service;

/**
 * 故障类型服务实现类
 */
@Service
public class FaultTypeServiceImpl extends ServiceImpl<FaultTypeMapper, FaultType> implements FaultTypeService {
}