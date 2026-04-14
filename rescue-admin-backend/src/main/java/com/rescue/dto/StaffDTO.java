package com.rescue.dto;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;

/**
 * 救援人员新增/编辑DTO
 * 仅包含前端可传递的字段，核心字段由后端控制
 */
@Data
public class StaffDTO {
    /**
     * 人员ID（编辑时必填，新增时为空）
     */
    private Integer id;

    /**
     * 姓名（必填）
     */
    @NotBlank(message = "救援人员姓名不能为空")
    private String name;

    /**
     * 联系电话（必填，手机号格式）
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确，请输入11位有效手机号")
    private String phone;

    /**
     * 擅长救援类型（必填）
     */
    @NotBlank(message = "擅长救援类型不能为空")
    private String skill;
}