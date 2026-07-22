package com.koala.config;

import com.koala.common.auth.AuthInterceptor;
import com.koala.common.web.MaintenanceInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final MaintenanceInterceptor maintenanceInterceptor;

    /**
     * 允许跨域的来源列表；dev 缺省是 "*"，prod 必须显式列出。
     * 多个用英文逗号分隔，例如：https://admin.example.com,https://m.example.com
     */
    @Value("${koala.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    public WebMvcConfig(AuthInterceptor authInterceptor, MaintenanceInterceptor maintenanceInterceptor) {
        this.authInterceptor = authInterceptor;
        this.maintenanceInterceptor = maintenanceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**");
        // 维护模式拦截器置于鉴权之后(见 6.9)
        registry.addInterceptor(maintenanceInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
