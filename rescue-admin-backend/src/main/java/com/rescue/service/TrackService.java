package com.rescue.service;

import com.rescue.util.Result;
import java.util.Map;

public interface TrackService {
    Result<?> uploadSupplierLocation(String orderNo, Double supplierLat, Double supplierLng);
    Result<Map<String, Double>> getSupplierLocation(String orderNo);
}