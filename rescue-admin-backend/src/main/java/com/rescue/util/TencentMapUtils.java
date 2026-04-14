package com.rescue.util;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 腾讯地图接口工具类（仅实现核心的地址→经纬度解析）
 */
@Component
public class TencentMapUtils {

    // 加默认值，避免启动失败
    @Value("${tencent.map.key:}")
    private String mapKey;

    // 接口地址固定，加默认值兜底
    @Value("${tencent.map.geo-url:https://apis.map.qq.com/ws/geocoder/v1/}")
    private String geoUrl;

    /**
     * 核心方法：地址转经纬度
     * @param province 省份（如：广东省）
     * @param city 城市（如：深圳市）
     * @param region 区县（如：南山区）
     * @param detail 详细地址（如：科技园路1号）
     * @return 经纬度封装对象（lng=经度，lat=纬度），解析失败返回null
     */
    public LngLatVO addressToLngLat(String province, String city, String region, String detail) {

        try {
            Thread.sleep(200); // 延迟200ms，避免瞬间并发
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 1. 简化Key校验：只判断是否为空（彻底解决误判问题）
        if (mapKey == null || mapKey.trim().isEmpty()) {
            System.err.println("腾讯地图Key未配置！请在application.yml中配置tencent.map.key");
            return null;
        }

        // 2. 拼接完整地址（过滤空值 + 提升解析准确率）
        String fullAddress = "";
        if (province != null && !province.trim().isEmpty()) {
            fullAddress += province.trim();
        }
        if (city != null && !city.trim().isEmpty()) {
            fullAddress += city.trim();
        }
        if (region != null && !region.trim().isEmpty()) {
            fullAddress += region.trim();
        }
        if (detail != null && !detail.trim().isEmpty()) {
            fullAddress += detail.trim();
        }
        // 地址长度判断：至少包含省+市+详细地址（比如「广东省深圳市xx路」）
        if (fullAddress.trim().length() < 6) {
            System.err.println("地址过短，无法解析：" + fullAddress);
            return null;
        }

        // 3. 构建请求参数（中文地址URL编码，简化写法）
        Map<String, Object> params = new HashMap<>();
        params.put("address", URLUtil.encode(fullAddress, StandardCharsets.UTF_8)); // 推荐写法
        params.put("key", mapKey);
        params.put("output", "json");

        try {
            // 4. 调用腾讯地图地理编码接口（GET请求）
            String result = HttpUtil.get(geoUrl, params);
            JSONObject resultJson = JSONObject.parseObject(result);

            // 5. 解析返回结果（腾讯地图返回码：0=成功）
            int status = resultJson.getInteger("status");
            if (status != 0) {
                System.err.println("腾讯地图解析失败：" + resultJson.getString("message") + "，地址：" + fullAddress);
                return null;
            }

            // 6. 提取经纬度
            JSONObject location = resultJson.getJSONObject("result").getJSONObject("location");
            Double lng = location.getDouble("lng"); // 经度
            Double lat = location.getDouble("lat"); // 纬度

            // 7. 返回封装结果
            LngLatVO lngLatVO = new LngLatVO();
            lngLatVO.setLng(lng);
            lngLatVO.setLat(lat);
            return lngLatVO;

        } catch (Exception e) {
            System.err.println("腾讯地图接口调用异常：" + e.getMessage() + "，地址：" + fullAddress);
            e.printStackTrace(); // 新增：打印完整异常栈，方便排查
            return null;
        }
    }

    /**
     * 内部封装类：存储经纬度
     */
    public static class LngLatVO {
        private Double lng; // 经度
        private Double lat; // 纬度

        // 手动生成get/set（避免Lombok问题）
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
    }
}