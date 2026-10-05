package com.library.config;

import com.library.audit.ApiWriteAuditInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiWriteAuditInterceptor apiWriteAuditInterceptor;

    public WebMvcConfig(ApiWriteAuditInterceptor apiWriteAuditInterceptor) {
        this.apiWriteAuditInterceptor = apiWriteAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Authentication endpoints are audited by AuthenticationAuditListener
        registry.addInterceptor(apiWriteAuditInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/auth/**");
    }
}
