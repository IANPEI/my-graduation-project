package com.rescue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rescue.entity.Order;
import com.rescue.entity.Staff;
import com.rescue.entity.Supplier;
import com.rescue.entity.User;
import com.rescue.mapper.*;
import com.rescue.service.SupplierManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SupplierManageServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierManageService {

    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private UserMapper userMapper;

    // 补全管理员工作台数据
    @Override
    public Map<String, Object> getAdminHomeData() {
        Map<String, Object> data = new HashMap<>();
        // 1. 服务商统计
        LambdaQueryWrapper<Supplier> sWrapper = new LambdaQueryWrapper<>();
        data.put("supplierCount", supplierMapper.selectCount(sWrapper));
        // 重置wrapper，避免条件叠加
        sWrapper.clear();
        sWrapper.apply("DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')");
        data.put("monthNewSupplierCount", supplierMapper.selectCount(sWrapper));

        // 2. 订单统计
        LambdaQueryWrapper<Order> oWrapper = new LambdaQueryWrapper<>();
        data.put("totalOrderCount", orderMapper.selectCount(oWrapper));
        // 重置wrapper，避免条件叠加
        oWrapper.clear();
        oWrapper.apply("DATE(create_time) = CURDATE()");
        data.put("todayNewOrderCount", orderMapper.selectCount(oWrapper));

        // 3. 新增：近7天订单趋势数据（核心补充）
        Map<String, Object> orderTrendData = getOrderTrendData();
        data.put("orderTrendData", orderTrendData);

        // 4. 人员统计
        LambdaQueryWrapper<Staff> stWrapper = new LambdaQueryWrapper<>();
        data.put("staffTotalCount", staffMapper.selectCount(stWrapper));
        // 重置wrapper，避免条件叠加
        stWrapper.clear();
        stWrapper.eq(Staff::getStatus, "online");
        data.put("onlineStaffCount", staffMapper.selectCount(stWrapper));

        // 5. 用户统计
        LambdaQueryWrapper<User> uWrapper = new LambdaQueryWrapper<>();
        data.put("userCount", userMapper.selectCount(uWrapper));
        // 重置wrapper，避免条件叠加
        uWrapper.clear();
        uWrapper.apply("DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')");
        data.put("monthNewUserCount", userMapper.selectCount(uWrapper));

        // 6. 最新申请
        data.put("supplierApplyList", getLatestApplyList());

        return data;
    }

    /**
     * 新增：获取近7天订单趋势数据
     * @return 包含dates和counts的Map
     */
    private Map<String, Object> getOrderTrendData() {
        Map<String, Object> trendData = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        // 1. 生成近7天的日期（格式：YYYY-MM-DD），从7天前到今天
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            Date date = cal.getTime();
            dates.add(sdf.format(date));
        }

        // 2. 查询近7天的订单数据（按日期分组）
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.apply("DATE(create_time) >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)")
                .groupBy("DATE(create_time)")
                .select("DATE(create_time) as order_date, COUNT(order_no) as order_count");

        // 执行查询，返回Map列表（key: order_date, order_count）
        List<Map<String, Object>> orderList = orderMapper.selectMaps(wrapper);

        // 3. 构建日期-订单数的映射
        Map<String, Integer> orderMap = new HashMap<>();
        for (Map<String, Object> map : orderList) {
            String orderDate = map.get("order_date").toString();
            Integer orderCount = Integer.parseInt(map.get("order_count").toString());
            orderMap.put(orderDate, orderCount);
        }

        // 4. 补全每天的订单数（没有订单的日期填0）
        for (String date : dates) {
            counts.add(orderMap.getOrDefault(date, 0));
        }

        // 5. 封装返回数据
        trendData.put("dates", dates);
        trendData.put("counts", counts);
        return trendData;
    }

    // 补全服务商列表查询
    @Override
    public IPage<Supplier> getSupplierList(Page<Supplier> page, String name, String status) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) wrapper.like(Supplier::getName, name);
        if (status != null && !status.isEmpty()) wrapper.eq(Supplier::getStatus, status);
        wrapper.orderByDesc(Supplier::getCreateTime);
        return supplierMapper.selectPage(page, wrapper);
    }

    // 补全服务商状态切换
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeSupplierStatus(String id, String status) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) throw new RuntimeException("服务商不存在");
        if (!"enable".equals(status) && !"disable".equals(status)) throw new RuntimeException("状态仅支持enable/disable");
        supplier.setStatus(status);
        supplier.setUpdateTime(new Date());
        supplierMapper.updateById(supplier);
    }

    // 补全最新申请列表
    @Override
    public List<Supplier> getLatestApplyList() {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getStatus, "pending")
                .orderByDesc(Supplier::getCreateTime)
                .last("LIMIT 10");
        return supplierMapper.selectList(wrapper);
    }


    /**
     * 根据ID查询服务商详情
     */
    @Override
    public Supplier getSupplierById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new RuntimeException("服务商ID不能为空");
        }
        return this.getById(id);
    }

    @Override
    public boolean checkSupplierIdUnique(String id) {
        if (!StringUtils.hasText(id)) {
            return false;
        }
        // 查询是否存在该ID的服务商
        LambdaQueryWrapper<Supplier> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Supplier::getId, id);
        return this.count(queryWrapper) == 0;
    }

    /**
     * 新增服务商
     */
    @Override
    public boolean saveSupplier(Supplier supplier) {
        if (supplier == null) {
            throw new RuntimeException("服务商信息不能为空");
        }

        // 新增时强制校验ID唯一性
        if (!StringUtils.hasText(supplier.getId())) {
            throw new RuntimeException("服务商ID不能为空");
        }
        if (!checkSupplierIdUnique(supplier.getId())) {
            throw new RuntimeException("服务商ID已存在，请更换");
        }

        // 设置默认值
        if (!StringUtils.hasText(supplier.getStatus())) {
            supplier.setStatus("pending"); // 默认待审核
        }
        Date now = new Date();
        supplier.setCreateTime(now);
        supplier.setUpdateTime(now);
        return this.save(supplier);
    }

    /**
     * 编辑服务商
     */
    @Override
    public boolean updateSupplier(Supplier supplier) {
        if (supplier == null || !StringUtils.hasText(supplier.getId())) {
            throw new RuntimeException("服务商ID不能为空");
        }

        // 编辑时禁止修改ID（前端已禁用，后端二次校验）
        Supplier oldSupplier = getById(supplier.getId());
        if (oldSupplier == null) {
            throw new RuntimeException("服务商不存在");
        }
        supplier.setUpdateTime(new Date()); // 更新时间
        return this.updateById(supplier);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSupplier(String supplierId, String status) {
        // 1. 更新服务商表状态
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setStatus(status);
        supplier.setUpdateTime(new Date());
        supplierMapper.updateById(supplier);

        // 2. 更新用户表状态
        // enable → 用户启用(1)
        // disable → 用户禁用(0)
        Integer userStatus = "enable".equals(status) ? 1 : 0;

        // 构建用户对象
        User user = new User();
        user.setStatus(userStatus);
        user.setUpdateTime(new Date());

        // 条件：根据 supplierId 匹配
        LambdaUpdateWrapper<User> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(User::getSupplierId, supplierId);
        userMapper.update(user, wrapper);
    }
}