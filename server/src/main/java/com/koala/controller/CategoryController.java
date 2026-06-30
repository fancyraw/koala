package com.koala.controller;

import com.koala.common.result.Result;
import com.koala.dto.product.CategoryView;
import com.koala.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "C端-商品分类")
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "启用分类列表")
    @GetMapping
    public Result<List<CategoryView>> list() {
        return Result.success(categoryService.listValid());
    }
}
