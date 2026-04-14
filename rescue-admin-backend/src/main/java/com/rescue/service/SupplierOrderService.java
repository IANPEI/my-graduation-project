package com.rescue.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rescue.dto.OrderDetailDTO;
import com.rescue.dto.OrderStatisticsDTO;
import com.rescue.dto.StaffDTO;
import com.rescue.entity.Order;
import com.rescue.entity.Staff;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface SupplierOrderService extends IService<Order> {

    void exportOrder(String orderNo, HttpServletResponse response);

    //1.工作台
    IPage<OrderDetailDTO> getSupplierOrderListWithFullDetail(Page<OrderDetailDTO> page, String supplierId);

    // 2. 订单列表查询
    IPage<OrderDetailDTO> getOrderList(
            Integer pageNum,
            Integer pageSize,
            String supplierId,
            String orderNo,
            String status,
            String startDate,
            String endDate
    );

    // 订单详情
    OrderDetailDTO getOrderDetail(String orderNo);

    // 3. 接单
    void acceptOrder(String orderNo, String supplierId,String staffId);

    //取消订单
    void cancelOrderBySupplier(String orderNo, String supplierId);

    // 4. 完成订单
    void completeOrder(String orderNo);

    // 5. 最新待处理订单
    List<Order> getLatestPendingOrders(String supplierId);

    // 6. 救援人员列表
    IPage<Staff> getStaffList(Page<Staff> page, String supplierId, String name);

    // 7. 切换人员状态
    void changeStaffStatus(String supplierId, String id, String status);

    // 8. 删除救援人员
    boolean deleteStaff(String supplierId, String id);

    /**
     * 新增救援人员
     */
    void addStaff(String supplierId, StaffDTO staffDTO);

    /**
     * 编辑救援人员
     */
    void editStaff(String supplierId, StaffDTO staffDTO);


    // 统计待处理订单数（未分配给当前服务商、状态为pending）
    long countPendingOrders(String supplierId);

    // 查询最新待处理订单（前N条）
    List<OrderDetailDTO> listLatestPendingOrders(String supplierId, int limit);

    OrderStatisticsDTO getOrderStatistics(String supplierId);
}