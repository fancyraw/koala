package com.koala.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.koala.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_address")
public class UserAddress extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String province;
    private String city;
    private String district;
    private String detail;
    private String fullAddress;
    private Integer isDefault;
}
