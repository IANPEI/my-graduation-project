package com.rescue.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 订单统计DTO
 * 适配前端ECharts所需的统计数据格式
 */
@Data
public class OrderStatisticsDTO {
    // 故障类型分布：key=故障类型名称，value=数量
    private Map<String, Integer> faultTypeDistribution;

    // 近7日订单数量：List按日期排序，每个元素是{date: "03-23", count: 5}
    private List<Map<String, Object>> last7DaysOrderCount;

    // 可选：订单状态分布（pending/processing/completed）
    private Map<String, Integer> statusDistribution;
}