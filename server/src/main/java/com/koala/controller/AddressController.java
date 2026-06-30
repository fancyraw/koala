package com.koala.controller;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.dto.address.AddressSaveRequest;
import com.koala.dto.address.AddressView;
import com.koala.dto.address.IdRequest;
import com.koala.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "C端-收货地址")
@RestController
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @Operation(summary = "地址列表(默认地址置顶)")
    @GetMapping
    public Result<List<AddressView>> list() {
        return Result.success(addressService.list(AuthContext.requireUserId()));
    }

    @Operation(summary = "地址详情")
    @GetMapping("/detail")
    public Result<AddressView> detail(@RequestParam Long id) {
        return Result.success(addressService.detail(AuthContext.requireUserId(), id));
    }

    @Operation(summary = "新增地址")
    @PostMapping("/add")
    public Result<Long> add(@Valid @RequestBody AddressSaveRequest req) {
        return Result.success(addressService.add(AuthContext.requireUserId(), req));
    }

    @Operation(summary = "编辑地址")
    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody AddressSaveRequest req) {
        addressService.update(AuthContext.requireUserId(), req);
        return Result.success();
    }

    @Operation(summary = "删除地址")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdRequest req) {
        addressService.delete(AuthContext.requireUserId(), req.getId());
        return Result.success();
    }
}
