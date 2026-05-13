package com.rescue.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.dto.OrderStatisticsDTO;
import com.rescue.dto.StaffDTO;
import com.rescue.entity.Order;
import com.rescue.entity.OrderEvaluate;
import com.rescue.entity.Staff;
import com.rescue.entity.User;
import com.rescue.mapper.SupplierMapper;
import com.rescue.service.*;
import com.rescue.util.JwtUtil;
import com.rescue.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 服务商控制器
 * 处理服务商端订单、人员相关接口（适配JWT认证 + 统一Result返回）
 */
@RestController
@RequestMapping("/supplier")
public class SupplierController {

    @Autowired
    private SupplierOrderService supplierOrderService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    // ===================== 核心：从JWT Token解析当前登录服务商ID =====================
    /**
     * 从请求头的JWT Token中解析当前登录服务商的supplier_id
     * 异常时直接抛出RuntimeException，由上层统一返回Result.error
     */
    private String getCurrentSupplierId() {
        // 1. 从请求头获取Token（格式：Bearer {token}）
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("未获取到登录凭证，请重新登录");
        }
        token = token.substring(7).trim(); // 去掉Bearer前缀

        // 2. 校验Token是否过期
        if (jwtUtil.isTokenExpired(token)) {
            throw new RuntimeException("登录凭证已过期，请重新登录");
        }

        // 3. 解析Token获取用户ID
        String userId = jwtUtil.extractUserId(token);
        if (!StringUtils.hasText(userId)) {
            throw new RuntimeException("Token解析失败，用户ID为空");
        }

        // 4. 查询用户信息，校验身份并获取supplierId
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在，请重新登录");
        }
        if (user.getIdentityType() != 2) { // 2=服务商身份
            throw new RuntimeException("当前用户不是服务商，无权限访问");
        }
        String supplierId = user.getSupplierId();
        if (!StringUtils.hasText(supplierId)) {
            throw new RuntimeException("该服务商账号未绑定supplierId，请联系管理员");
        }

        return supplierId;
    }


    // 1. 服务商工作台数据
    @GetMapping("/home")
    public Result<IPage<OrderDetailDTO>> getSupplierOrderList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            String supplierId = getCurrentSupplierId(); // 从Token解析服务商ID，替代车主端的userId
            Page<OrderDetailDTO> page = new Page<>(pageNum, pageSize);
            IPage<OrderDetailDTO> orderPage = supplierOrderService.getSupplierOrderListWithFullDetail(page, supplierId);
            return Result.success(orderPage);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    //导出订单
    @GetMapping("/export/{orderNo}")
    public void exportOrder(
            @PathVariable String orderNo,
            HttpServletResponse response) {

        supplierOrderService.exportOrder(orderNo, response);
    }

    // 2. 获取订单列表（分页 + 条件查询）（小程序端）
    @GetMapping("/order/list")
    public Result<IPage<OrderDetailDTO>> getOrderList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {

        String supplierId = getCurrentSupplierId();

        IPage<OrderDetailDTO> page =
                supplierOrderService.getOrderList(
                        pageNum,
                        pageSize,
                        supplierId,
                        orderNo,
                        status,
                        startDate,
                        endDate
                );

        return Result.success(page);
    }

    // 订单详情
    @GetMapping("/order/detail/{orderNo}")
    public Result<OrderDetailDTO> getOrderDetail(@PathVariable String orderNo) {
        return Result.success(supplierOrderService.getOrderDetail(orderNo));
    }


    @Autowired
    private OrderEvaluateService orderEvaluateService;
    @GetMapping("/evaluate/{orderNo}")
    public Result getEvaluate(@PathVariable String orderNo) {
        OrderEvaluate evaluate = orderEvaluateService.getByOrderNo(orderNo);
        return Result.success(evaluate);
    }

    // 3. 接单
    @PostMapping("/order/accept/{orderNo}")
    public Result<?> acceptOrder(
            @PathVariable String orderNo,
            @RequestBody Map<String, String> params) {

        String supplierId = getCurrentSupplierId();
        String staffId = params.get("staffId"); // ✅ 接收前端传的师傅ID
        supplierOrderService.acceptOrder(orderNo, supplierId, staffId);
        return Result.success();
    }

    // 4. 完成订单
    @PostMapping("/order/complete/{orderNo}")
    public Result<?> completeOrder(@PathVariable String orderNo) {

        supplierOrderService.completeOrder(orderNo);
        return Result.success();
    }

    // 服务商取消已接单的订单
    @PostMapping("/order/cancel/{orderNo}")
    public Result<?> cancelOrder(@PathVariable String orderNo) {
        try {
            String supplierId = getCurrentSupplierId();
            // 调用业务层取消订单（校验：必须是当前服务商的 processing 订单）
            supplierOrderService.cancelOrderBySupplier(orderNo, supplierId);
            return Result.success("取消成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 5. 获取救援人员列表（分页 + 姓名搜索）
    @GetMapping("/staff/list")
    public Result<IPage<Staff>> getStaffList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String name) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        try {
            String supplierId = getCurrentSupplierId();
            Page<Staff> page = new Page<>(pageNum, pageSize);
            // 仅查询当前服务商的救援人员
            IPage<Staff> staffPage = supplierOrderService.getStaffList(page, supplierId, name);
            return Result.success(staffPage);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 6. 切换人员在线/离线状态
    @PostMapping("/staff/status/{id}")
    public Result<Void> changeStaffStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> params) {
        // 参数校验
        if (!StringUtils.hasText(id)) {
            return Result.error("人员ID不能为空");
        }
        String status = params.get("status");
        if (!StringUtils.hasText(status) || (!"online".equals(status) && !"offline".equals(status))) {
            return Result.error("状态参数非法，仅支持online/offline");
        }

        try {
            String supplierId = getCurrentSupplierId();
            supplierOrderService.changeStaffStatus(supplierId, id, status);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 7. 删除救援人员（校验服务商归属）
    @DeleteMapping("/staff/delete/{id}")
    public Result<Void> deleteStaff(@PathVariable String id) {
        if (!StringUtils.hasText(id)) {
            return Result.error("人员ID不能为空");
        }

        try {
            String supplierId = getCurrentSupplierId();
            // 校验人员归属后删除
            boolean success = supplierOrderService.deleteStaff(supplierId, id);
            if (success) {
                return Result.success();
            } else {
                return Result.error("删除失败，人员不存在或不属于当前服务商");
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增救援人员
     */
    @PostMapping("/staff/add")
    public Result<Void> addStaff(@Valid @RequestBody StaffDTO staffDTO) {
        try {
            String supplierId = getCurrentSupplierId();
            supplierOrderService.addStaff(supplierId, staffDTO);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 编辑救援人员
     */
    @PostMapping("/staff/edit")
    public Result<Void> editStaff(@Valid @RequestBody StaffDTO staffDTO) {
        try {
            String supplierId = getCurrentSupplierId();
            supplierOrderService.editStaff(supplierId, staffDTO);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }


    //小程序端

    @Autowired
    private SupplierService supplierService;

    // 返回工作台聚合数据
    @GetMapping("/home/simple")
    public Result<Map<String, Object>> getSupplierHomeSimple() {
        try {
            String supplierId = getCurrentSupplierId();

            // 1. 查询待处理订单数
            long pendingCount = supplierOrderService.countPendingOrders(supplierId);

            // 2. 查询最新的待处理订单（前5条）
            // 注意：待接单不能查完整DTO，必须查简化版！
            List<OrderDetailDTO> pendingOrders = supplierOrderService.listLatestPendingOrders(supplierId, 5);

            // 3. 组装返回数据
            Map<String, Object> result = Map.of(
                    "pendingCount", pendingCount,
                    "orderList", pendingOrders
            );
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取服务商资料
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getSupplierProfile() {
        try {
            String supplierId = getCurrentSupplierId();
            // 从supplier表查询资料（需注入SupplierService）
            Map<String, Object> profile = supplierService.getSupplierProfileById(supplierId);
            return Result.success(profile);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改服务商资料
     */
    @PostMapping("/profile/update")
    public Result<Void> updateSupplierProfile(@RequestBody Map<String, String> params) {
        try {
            String supplierId = getCurrentSupplierId();
            supplierService.updateSupplierProfile(supplierId, params);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单统计数据
     */
    @GetMapping("/order/statistics")
    public Result<OrderStatisticsDTO> getOrderStatistics() {
        try {
            String supplierId = getCurrentSupplierId();
            OrderStatisticsDTO statistics = supplierOrderService.getOrderStatistics(supplierId);
            return Result.success(statistics);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

}