package com.koala.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.Admin;
import com.koala.mapper.AdminMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 超管初始化（鸡生蛋）：配置 koala.admin.super-openid 后，启动幂等建超管账号。
 */
@Slf4j
@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private final AdminProperties adminProps;
    private final AdminMapper adminMapper;

    public SuperAdminInitializer(AdminProperties adminProps, AdminMapper adminMapper) {
        this.adminProps = adminProps;
        this.adminMapper = adminMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        String openid = adminProps.getSuperOpenid();
        if (!StringUtils.hasText(openid)) {
            log.warn("未配置 koala.admin.super-openid，跳过超管初始化");
            return;
        }
        Long exists = adminMapper.selectCount(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getWxOpenid, openid));
        if (exists != null && exists > 0) {
            return;
        }
        Admin admin = new Admin();
        admin.setWxOpenid(openid);
        admin.setNickname("超级管理员");
        admin.setAvatarUrl("");
        admin.setIsSuper(1);
        admin.setIsValid(1);
        adminMapper.insert(admin);
        log.info("已初始化超级管理员账号 openid={}", openid);
    }
}
