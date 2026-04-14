package com.rescue.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rescue.entity.FaultType;
import com.rescue.entity.Supplier;
import com.rescue.entity.SystemConfig;
import com.rescue.entity.User;
import com.rescue.service.FaultTypeService;
import com.rescue.service.SupplierManageService;
import com.rescue.service.SystemConfigService;
import com.rescue.service.UserService;
import com.rescue.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private SupplierManageService supplierManageService;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private FaultTypeService faultTypeService;

    // 1. 管理员工作台数据
    @GetMapping("/home")
    public Result<Map<String, Object>> getHomeData() {
        Map<String, Object> data = supplierManageService.getAdminHomeData(); // 调用新增的管理员工作台方法
        return Result.success(data);
    }

    /**
     * 服务商入驻审核
     * 同时修改 t_supplier 状态 + t_user 状态
     * status: enable=通过, disable=拒绝
     */
    @PostMapping("/supplier/audit/{id}")
    public Result<Void> auditSupplier(@PathVariable String id, @RequestBody Map<String, String> params) {
        try {
            String status = params.get("status");
            supplierManageService.auditSupplier(id, status);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Autowired
    private UserService userService;
    @GetMapping("/user/list")
    public Result<IPage<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String account,
            Integer identityType) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        if (StrUtil.isNotBlank(account)) {
            wrapper.like(User::getAccount, account);
        }
        if (identityType != null) {
            wrapper.eq(User::getIdentityType, identityType);
        }
        IPage<User> userPage = userService.page(page, wrapper);
        return Result.success(userPage);
    }

    // 2. 获取服务商列表（分页 + 条件查询）
    @GetMapping("/supplier/list")
    public Result<IPage<Supplier>> getSupplierList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String name,
            String status) {
        Page<Supplier> page = new Page<>(pageNum, pageSize);
        IPage<Supplier> supplierPage = supplierManageService.getSupplierList(page, name, status); // 调用服务商列表方法
        return Result.success(supplierPage);
    }

    // 3. 切换服务商启用/禁用状态
    @PostMapping("/supplier/status/{id}")
    public Result<Void> changeSupplierStatus(@PathVariable String id, @RequestBody Map<String, String> params) {
        try {
            supplierManageService.changeSupplierStatus(id, params.get("status")); // 调用状态切换方法
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }


    // 8. 获取服务商详情
    @GetMapping("/supplier/detail/{id}")
    public Result<Supplier> getSupplierDetail(@PathVariable String id) {
        try {
            Supplier supplier = supplierManageService.getSupplierById(id);
            return Result.success(supplier);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 检验服务商ID是否唯一
    @GetMapping("/supplier/checkIdUnique")
    public Result<Boolean> checkSupplierIdUnique(String id) {
        try {
            boolean isUnique = supplierManageService.checkSupplierIdUnique(id);
            return Result.success(isUnique);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 9. 新增服务商
    @PostMapping("/supplier/save")
    public Result<Void> saveSupplier(@RequestBody Supplier supplier) {
        try {
            supplierManageService.saveSupplier(supplier);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 10. 编辑服务商
    @PostMapping("/supplier/update")
    public Result<Void> updateSupplier(@RequestBody Supplier supplier) {
        try {
            supplierManageService.updateSupplier(supplier);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 4. 获取系统配置
    @GetMapping("/system/config")
    public Result<SystemConfig> getSystemConfig() {
        SystemConfig config = systemConfigService.getById(1L);
        return Result.success(config);
    }

    // 5. 保存系统配置
    @PostMapping("/system/config/save")
    public Result<Void> saveSystemConfig(@RequestBody SystemConfig config) {
        config.setId(1L);
        systemConfigService.saveOrUpdate(config);
        return Result.success();
    }

    // 6. 获取故障类型列表
    @GetMapping("/system/faultType/list")
    public Result<List<FaultType>> getFaultTypeList() {
        List<FaultType> list = faultTypeService.list();
        return Result.success(list);
    }

    // 7. 删除故障类型
    @DeleteMapping("/system/faultType/delete/{id}")
    public Result<Void> deleteFaultType(@PathVariable Long id) {
        faultTypeService.removeById(id);
        return Result.success();
    }

    // 新增故障类型
    @PostMapping("/system/faultType/save")
    public Result<Void> saveFaultType(@RequestBody FaultType faultType) {
        try {
            // 补充创建时间（如果业务需要自动生成）
            if (faultType.getCreateTime() == null) {
                faultType.setCreateTime(new Date());
            }
            // 默认状态：1-启用（如果前端未传）
            if (faultType.getStatus() == null) {
                faultType.setStatus(1);
            }
            faultTypeService.save(faultType);
            return Result.success();
        } catch (Exception e) {
            return Result.error("新增故障类型失败：" + e.getMessage());
        }
    }

    // 编辑故障类型
    @PostMapping("/system/faultType/update")
    public Result<Void> updateFaultType(@RequestBody FaultType faultType) {
        try {
            // 校验主键是否存在
            if (faultType.getId() == null) {
                return Result.error("故障类型ID不能为空");
            }
            // 不修改创建时间（保持原有创建时间）
            FaultType oldFaultType = faultTypeService.getById(faultType.getId());
            if (oldFaultType != null) {
                faultType.setCreateTime(oldFaultType.getCreateTime());
            }
            faultTypeService.updateById(faultType);
            return Result.success();
        } catch (Exception e) {
            return Result.error("编辑故障类型失败：" + e.getMessage());
        }
    }

    //根据ID获取故障类型详情（编辑时回显数据）
    @GetMapping("/system/faultType/detail/{id}")
    public Result<FaultType> getFaultTypeDetail(@PathVariable Long id) {
        try {
            FaultType faultType = faultTypeService.getById(id);
            if (faultType == null) {
                return Result.error("故障类型不存在");
            }
            return Result.success(faultType);
        } catch (Exception e) {
            return Result.error("获取故障类型详情失败：" + e.getMessage());
        }
    }
}