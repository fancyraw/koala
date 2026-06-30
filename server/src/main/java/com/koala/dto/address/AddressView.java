package com.koala.dto.address;

import com.koala.entity.UserAddress;
import lombok.Data;

@Data
public class AddressView {

    private Long id;
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
    private boolean isDefault;

    public static AddressView of(UserAddress a) {
        AddressView v = new AddressView();
        v.id = a.getId();
        v.name = a.getName();
        v.phone = a.getPhone();
        v.provinceCode = a.getProvinceCode();
        v.cityCode = a.getCityCode();
        v.districtCode = a.getDistrictCode();
        v.province = a.getProvince();
        v.city = a.getCity();
        v.district = a.getDistrict();
        v.detail = a.getDetail();
        v.fullAddress = a.getFullAddress();
        v.isDefault = a.getIsDefault() != null && a.getIsDefault() == 1;
        return v;
    }
}
