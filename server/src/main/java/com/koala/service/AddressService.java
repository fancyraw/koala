package com.koala.service;

import com.koala.dto.address.AddressSaveRequest;
import com.koala.dto.address.AddressView;

import java.util.List;

public interface AddressService {

    /** 当前用户全部地址，默认地址置顶。 */
    List<AddressView> list(Long userId);

    /** 地址详情，校验归属当前用户。 */
    AddressView detail(Long userId, Long id);

    /** 新增地址（校验三级区划码），返回主键。设为默认时清除原默认。 */
    Long add(Long userId, AddressSaveRequest req);

    /** 编辑地址（校验归属与三级区划码）。 */
    void update(Long userId, AddressSaveRequest req);

    /** 删除地址（校验归属）。 */
    void delete(Long userId, Long id);
}
