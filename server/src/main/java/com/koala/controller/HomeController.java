package com.koala.controller;

import com.koala.common.auth.AuthContext;
import com.koala.common.result.Result;
import com.koala.dto.content.HomeView;
import com.koala.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "C端-首页")
@RestController
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @Operation(summary = "首页聚合(Banner+品类+热销+推荐+自动下发可领券)")
    @GetMapping("/home")
    public Result<HomeView> home() {
        return Result.success(homeService.home(AuthContext.requireUserId()));
    }
}
