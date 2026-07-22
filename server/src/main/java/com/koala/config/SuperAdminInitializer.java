package com.koala.config;

import com.koala.entity.Admin;
import com.koala.enums.ValidFlag;
import com.koala.repository.AdminRepository;
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
    private final AdminRepository adminRepository;

    public SuperAdminInitializer(AdminProperties adminProps, AdminRepository adminRepository) {
        this.adminProps = adminProps;
        this.adminRepository = adminRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        String openid = adminProps.getSuperOpenid();
        if (!StringUtils.hasText(openid)) {
            log.warn("未配置 koala.admin.super-openid，跳过超管初始化");
            return;
        }
        if (adminRepository.existsByOpenid(openid)) {
            return;
        }
        Admin admin = new Admin();
        admin.setWxOpenid(openid);
        admin.setNickname("超级管理员");
        admin.setAvatarUrl("");
        admin.setIsSuper(ValidFlag.ENABLED.code());
        admin.setIsValid(ValidFlag.ENABLED.code());
        adminRepository.insert(admin);
        log.info("已初始化超级管理员账号");
    }
}
