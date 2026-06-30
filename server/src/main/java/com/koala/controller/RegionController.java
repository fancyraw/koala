package com.koala.controller;

import com.koala.common.result.Result;
import com.koala.dto.region.RegionNode;
import com.koala.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "C端-行政区划")
@RestController
@RequestMapping("/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @Operation(summary = "省市区联动:取某父级下子区划(parent为空取省级)")
    @GetMapping
    public Result<List<RegionNode>> list(@RequestParam(required = false) String parent) {
        return Result.success(regionService.listChildren(parent));
    }
}
