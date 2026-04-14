package com.rescue.dto;

import lombok.Data;

@Data
public class UserAddressVO {
    private Long id;
    private String name;
    private String phone;
    private String province;
    private String city;
    private String region;
    private String detail;
    private Integer isDefault;
}
