package com.koala.controller.admin;

import com.koala.common.result.Result;
import com.koala.common.web.IdRequest;
import com.koala.dto.content.BannerSaveRequest;
import com.koala.dto.content.BannerView;
import com.koala.dto.product.SortRequest;
import com.koala.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "后台-Banner")
@RestController
@RequestMapping("/admin/banners")
public class AdminBannerController {

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @Operation(summary = "Banner列表(含下线)")
    @GetMapping
    public Result<List<BannerView>> list() {
        return Result.success(bannerService.listAll());
    }

    @Operation(summary = "新增/编辑Banner")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody BannerSaveRequest req) {
        return Result.success(bannerService.save(req));
    }

    @Operation(summary = "删除Banner")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdRequest req) {
        bannerService.delete(req.getId());
        return Result.success();
    }

    @Operation(summary = "批量排序")
    @PostMapping("/sort")
    public Result<Void> sort(@Valid @RequestBody SortRequest req) {
        bannerService.sort(req.getItems());
        return Result.success();
    }
}
