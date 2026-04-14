package com.rescue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.dto.RegisterDTO;
import com.rescue.entity.Supplier;
import com.rescue.entity.User;
import com.rescue.mapper.SupplierMapper;
import com.rescue.mapper.UserMapper;
import com.rescue.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 根据账号查询用户
     */
    @Override
    public User getUserByAccount(String account) {
        return baseMapper.selectByAccount(account);
    }

    /**
     * 验证用户登录（明文密码对比，无加密）
     */
    @Override
    public User verifyLogin(String account, String password) {
        // 1. 根据账号查询用户
        User user = getUserByAccount(account);
        if (user == null) {
            return null; // 账号不存在
        }

        // 2. 验证用户状态（是否启用）
        if (user.getStatus() != 1) {
            return null; // 账号已禁用
        }

        // 3. 直接对比明文密码（不加密）
        if (!password.equals(user.getPassword())) {
            return null; // 密码错误
        }

        return user; // 验证通过
    }

    @Autowired
    private SupplierMapper supplierMapper;

    /**
     * 用户注册
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 1. 构建用户信息
        User user = new User();
        user.setAccount(dto.getAccount());
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getNickname());
        user.setPassword(dto.getPassword());
        user.setIdentityType(dto.getIdentityType());
        if (dto.getIdentityType() == 3) {
            // 车主：直接启用
            user.setStatus(1);
        } else {
            // 供应商：注册后禁用，需审核通过才能启用
            user.setStatus(0);
        }
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());

        // 2. 服务商：生成supplierId
        if (dto.getIdentityType() == 2) {
            // 供应商ID生成：SP + 7位数字，保证不重复
            String supplierId;
            Random random = new Random();
            do {
                // 生成 0000000 ~ 9999999 的7位数字
                int num = random.nextInt(10000000);
                supplierId = "SP" + String.format("%07d", num);
                // 查询数据库是否已存在
            } while (supplierMapper.selectById(supplierId) != null);

            user.setSupplierId(supplierId);

            // 插入服务商
            Supplier supplier = new Supplier();
            supplier.setId(supplierId);
            supplier.setName(dto.getSupplierName());
            supplier.setContact(dto.getContact());
            supplier.setPhone(dto.getPhone());
            supplier.setCity(dto.getCity());
            supplier.setAddress(dto.getAddress());
            supplier.setStatus("pending"); // 待审核
            supplier.setCreateTime(new Date());
            supplier.setUpdateTime(new Date());
            supplierMapper.insert(supplier);
        }

        // 3. 插入用户
        this.save(user);
    }
}