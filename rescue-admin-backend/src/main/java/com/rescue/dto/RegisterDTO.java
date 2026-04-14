package com.rescue.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String account;
    private String phone;
    private String nickname;
    private String password;
    private Integer identityType; // 2服务商 3车主
    private String supplierName;
    private String contact;
    private String city;
    private String address;
}