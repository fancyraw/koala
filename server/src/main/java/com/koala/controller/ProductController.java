package com.koala.controller;

import com.koala.common.result.PageResult;
import com.koala.common.result.Result;
import com.koala.dto.product.ProductCardView;
import com.koala.dto.product.ProductDetailView;
import com.koala.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Tag(name = "C端-商品")
@Validated
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "商品列表(分类/搜索/分页)")
    @GetMapping
    public Result<PageResult<ProductCardView>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 不能小于 1") long page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 200, message = "size 不能大于 200") long size) {
        return Result.success(productService.listForUser(categoryId, keyword, page, size));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/detail")
    public Result<ProductDetailView> detail(@RequestParam Long id) {
        return Result.success(productService.detailForUser(id));
    }
}
