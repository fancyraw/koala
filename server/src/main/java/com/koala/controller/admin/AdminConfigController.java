package com.koala.controller.admin;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.dto.content.ConfigSaveRequest;
import com.koala.entity.SysConfig;
import com.koala.service.ConfigService;
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

@Tag(name = "后台-系统配置")
@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {

    private final ConfigService configService;

    public AdminConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @Operation(summary = "读取某分组配置(配送/支付/订单超时/维护开关group=system)")
    @GetMapping
    public Result<List<SysConfig>> list(@RequestParam String group) {
        return Result.success(configService.listByGroup(group));
    }

    @Operation(summary = "保存分组配置(改策略不发版)")
    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody ConfigSaveRequest req) {
        Long adminId = AuthContext.requireAdminId();
        for (ConfigSaveRequest.ConfigItem item : req.getItems()) {
            configService.save(req.getGroup(), item.getKey(), item.getValue(), adminId);
        }
        return Result.success();
    }
}
