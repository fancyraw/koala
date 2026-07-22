package com.koala.controller.admin;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.common.web.IdRequest;
import com.koala.dto.product.AdminProductView;
import com.koala.dto.product.ProductSaveRequest;
import com.koala.dto.product.ProductStatusRequest;
import com.koala.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Tag(name = "后台-商品")
@Validated
@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "商品列表(分类/搜索/状态/分页)")
    @GetMapping
    public Result<PageResult<AdminProductView>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 不能小于 1") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 200, message = "size 不能大于 200") long size) {
        return Result.success(productService.listForAdmin(categoryId, keyword, status, page, size));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/detail")
    public Result<AdminProductView> detail(@RequestParam Long id) {
        return Result.success(productService.detailForAdmin(id));
    }

    @Operation(summary = "新增/编辑商品(含规格)")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody ProductSaveRequest req) {
        return Result.success(productService.save(req, AuthContext.requireAdminId()));
    }

    @Operation(summary = "删除商品")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdRequest req) {
        productService.delete(req.getId());
        return Result.success();
    }

    @Operation(summary = "上下架")
    @PostMapping("/status")
    public Result<Void> status(@Valid @RequestBody ProductStatusRequest req) {
        productService.setStatus(req.getId(), req.getValid());
        return Result.success();
    }
}
