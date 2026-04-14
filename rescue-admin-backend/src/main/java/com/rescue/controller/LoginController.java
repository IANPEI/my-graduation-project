package com.rescue.controller;

import com.rescue.entity.Supplier;
import com.rescue.entity.User;
import com.rescue.service.SupplierService;
import com.rescue.service.UserService;
import com.rescue.util.JwtUtil;
import com.rescue.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录控制器（支持管理员/服务商/车主三种身份登录）
 */
@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    // 新增：注入服务商Service
    @Autowired
    private SupplierService supplierService;

    /**
     * 登录接口（自动识别身份：管理员/服务商/车主）
     */
    @PostMapping
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // 1. 验证参数
        if (request.getAccount() == null || request.getAccount().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty()) {
            return Result.error("账号/密码不能为空");
        }

        // 2. 验证用户登录（账号+密码校验）
        User user = userService.verifyLogin(request.getAccount(), request.getPassword());
        if (user == null) {
            return Result.error("账号或密码错误，或账号已禁用");
        }

        // 3. 根据身份类型生成标识（新增车主身份适配）
        String identity;
        Integer identityType = user.getIdentityType();
        if (identityType == 1) {
            identity = "admin"; // 管理员
        } else if (identityType == 2) {
            identity = "supplier"; // 服务商
        } else if (identityType == 3) {
            identity = "owner"; // 新增：车主身份
        } else {
            return Result.error("身份类型异常（仅支持管理员/服务商/车主）");
        }

        // 4. 生成JWT Token（包含用户ID/账号/身份，提升安全性）
        String token = jwtUtil.generateToken(user.getId().toString(), user.getAccount(), identity);

        // 5. 构造返回数据（适配前端需要的字段）
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("account", user.getAccount());
        data.put("identity", identity); // admin/supplier/owner
        data.put("identityType", identityType); // 1/2/3（数字类型，前端更易处理）

        // 6. 补充用户信息（前端个人中心需要）
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("account", user.getAccount());
        userInfo.put("nickname", user.getNickname()); // 车主昵称
        userInfo.put("phone", user.getPhone());       // 车主手机号
        data.put("userInfo", userInfo);

        // 7. 服务商专属字段（仅服务商返回）
        if (identityType == 2 && user.getSupplierId() != null) {
            data.put("supplierId", user.getSupplierId());

            // 核心新增：根据supplierId查询t_supplier表，获取服务商名称
            Supplier supplier = supplierService.getById(user.getSupplierId());
            if (supplier != null) {
                // 返回服务商名称，前端可直接拼接"XXX服务商"
                data.put("supplierName", supplier.getName());
            } else {
                // 兜底：若未查询到服务商信息，返回默认值
                data.put("supplierName", "未知");
            }
        }

        return Result.success(data);
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    /**
     * 登录请求参数（保持原有结构，无需修改）
     */
    public static class LoginRequest {
        private String account;
        private String password;

        // Getter & Setter
        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}