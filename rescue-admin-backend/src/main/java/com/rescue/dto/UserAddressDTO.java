package com.rescue.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class UserAddressDTO {
    /** 地址ID（编辑时传，新增时不传） */
    private Long id;

    /** 收货人姓名 */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    /** 省份 */
    @NotBlank(message = "省份不能为空")
    private String province;

    /** 城市 */
    @NotBlank(message = "城市不能为空")
    private String city;

    /** 区县 */
    @NotBlank(message = "区县不能为空")
    private String region;

    /** 详细地址 */
    @NotBlank(message = "详细地址不能为空")
    private String detail;

    /** 是否默认地址（0-否，1-是） */
    private Integer isDefault = 0; // 默认非默认
}


