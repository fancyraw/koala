package com.koala.controller.admin;

import com.koala.common.result.Result;
import com.koala.common.web.IdRequest;
import com.koala.dto.product.SortRequest;
import com.koala.dto.product.TagSaveRequest;
import com.koala.dto.product.TagView;
import com.koala.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "后台-商品标签")
@RestController
@RequestMapping("/admin/tags")
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "标签列表")
    @GetMapping
    public Result<List<TagView>> list() {
        return Result.success(tagService.listAll());
    }

    @Operation(summary = "新增/编辑标签")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody TagSaveRequest req) {
        return Result.success(tagService.save(req));
    }

    @Operation(summary = "删除标签(引用数0可删)")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdRequest req) {
        tagService.delete(req.getId());
        return Result.success();
    }

    @Operation(summary = "批量排序")
    @PostMapping("/sort")
    public Result<Void> sort(@Valid @RequestBody SortRequest req) {
        tagService.sort(req.getItems());
        return Result.success();
    }
}
