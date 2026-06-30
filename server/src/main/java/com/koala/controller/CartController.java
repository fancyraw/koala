package com.koala.controller;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.dto.cart.CartAddRequest;
import com.koala.dto.cart.CartRemoveRequest;
import com.koala.dto.cart.CartUpdateRequest;
import com.koala.dto.cart.CartView;
import com.koala.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "C端-购物车")
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "购物车列表")
    @GetMapping
    public Result<CartView> list() {
        return Result.success(cartService.list(AuthContext.requireUserId()));
    }

    @Operation(summary = "加购(同规格累加)")
    @PostMapping("/add")
    public Result<CartView> add(@Valid @RequestBody CartAddRequest req) {
        return Result.success(cartService.add(AuthContext.requireUserId(), req));
    }

    @Operation(summary = "更新数量/勾选")
    @PostMapping("/update")
    public Result<CartView> update(@Valid @RequestBody CartUpdateRequest req) {
        return Result.success(cartService.update(AuthContext.requireUserId(), req));
    }

    @Operation(summary = "删除(批量)")
    @PostMapping("/remove")
    public Result<CartView> remove(@Valid @RequestBody CartRemoveRequest req) {
        return Result.success(cartService.remove(AuthContext.requireUserId(), req.getIds()));
    }
}
