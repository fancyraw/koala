package com.koala.service;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.address.AddressSaveRequest;
import com.koala.entity.Region;
import com.koala.entity.UserAddress;
import com.koala.enums.ValidFlag;
import com.koala.repository.UserAddressRepository;
import com.koala.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private UserAddressRepository addressRepository;
    @Mock
    private RegionService regionService;

    @InjectMocks
    private AddressServiceImpl service;

    private Region region(String code, String name, int level) {
        Region r = new Region();
        r.setCode(code);
        r.setName(name);
        r.setLevel(level);
        return r;
    }

    private AddressSaveRequest req() {
        AddressSaveRequest req = new AddressSaveRequest();
        req.setName("张三");
        req.setPhone("13800000000");
        req.setProvinceCode("11");
        req.setCityCode("1101");
        req.setDistrictCode("110101");
        req.setDetail("某街道1号");
        return req;
    }

    private void stubChain() {
        when(regionService.validateChain("11", "1101", "110101")).thenReturn(new Region[]{
                region("11", "北京市", 1),
                region("1101", "市辖区", 2),
                region("110101", "东城区", 3)
        });
    }

    @Test
    void add_firstAddress_forcedDefault_andFullAddressComposed() {
        stubChain();
        when(addressRepository.countByUser(1L)).thenReturn(0L);

        AddressSaveRequest req = req();
        req.setIsDefault(false); // 即便请求非默认，首个地址也强制为默认

        service.add(1L, req);

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(addressRepository).clearDefault(1L);
        verify(addressRepository).insert(captor.capture());
        UserAddress saved = captor.getValue();
        assertThat(saved.getIsDefault()).isEqualTo(ValidFlag.ENABLED.code());
        assertThat(saved.getFullAddress()).isEqualTo("北京市市辖区东城区某街道1号");
        assertThat(saved.getProvince()).isEqualTo("北京市");
    }

    @Test
    void add_notDefault_whenAlreadyHasAddresses_skipsClearDefault() {
        stubChain();
        when(addressRepository.countByUser(1L)).thenReturn(3L);

        AddressSaveRequest req = req();
        req.setIsDefault(false);

        service.add(1L, req);

        verify(addressRepository, never()).clearDefault(1L);
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(addressRepository).insert(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(ValidFlag.DISABLED.code());
    }

    @Test
    void add_explicitDefault_clearsExistingDefault() {
        stubChain();

        AddressSaveRequest req = req();
        req.setIsDefault(true);

        service.add(1L, req);

        verify(addressRepository).clearDefault(1L);
    }

    @Test
    void update_missingId_throwsParamMissing() {
        AddressSaveRequest req = req();
        req.setId(null);

        assertThatThrownBy(() -> service.update(1L, req))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.PARAM_MISSING.getCode()));
    }

    @Test
    void update_notOwned_throwsDataNotFound() {
        UserAddress other = new UserAddress();
        other.setId(5L);
        other.setUserId(999L);
        when(addressRepository.findById(5L)).thenReturn(other);

        AddressSaveRequest req = req();
        req.setId(5L);

        assertThatThrownBy(() -> service.update(1L, req))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void update_setDefault_clearsOthersAndEnables() {
        stubChain();
        UserAddress owned = new UserAddress();
        owned.setId(5L);
        owned.setUserId(1L);
        when(addressRepository.findById(5L)).thenReturn(owned);

        AddressSaveRequest req = req();
        req.setId(5L);
        req.setIsDefault(true);

        service.update(1L, req);

        verify(addressRepository).clearDefault(1L);
        verify(addressRepository).updateById(owned);
        assertThat(owned.getIsDefault()).isEqualTo(ValidFlag.ENABLED.code());
    }

    @Test
    void delete_notOwned_throwsAndSkipsDelete() {
        UserAddress other = new UserAddress();
        other.setId(5L);
        other.setUserId(999L);
        when(addressRepository.findById(5L)).thenReturn(other);

        assertThatThrownBy(() -> service.delete(1L, 5L))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
        verify(addressRepository, never()).deleteById(5L);
    }

    @Test
    void delete_owned_deletes() {
        UserAddress owned = new UserAddress();
        owned.setId(5L);
        owned.setUserId(1L);
        when(addressRepository.findById(5L)).thenReturn(owned);

        service.delete(1L, 5L);

        verify(addressRepository).deleteById(5L);
    }
}
