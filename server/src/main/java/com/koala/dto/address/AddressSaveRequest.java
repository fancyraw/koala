package com.koala.dto.address;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class AddressSaveRequest {

    /** 新增时为空；编辑时必填。 */
    private Long id;

    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 32, message = "收货人姓名过长")
    private String name;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "省级区划码不能为空")
    private String provinceCode;

    @NotBlank(message = "市级区划码不能为空")
    private String cityCode;

    @NotBlank(message = "区/县级区划码不能为空")
    private String districtCode;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 256, message = "详细地址过长")
    private String detail;

    /** 是否设为默认地址。 */
    private Boolean isDefault;
}
