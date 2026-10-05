package com.library.config;

import com.library.audit.ApiWriteAuditInterceptor;
import com.library.security.RecentAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiWriteAuditInterceptor apiWriteAuditInterceptor;
    private final RecentAuthenticationInterceptor recentAuthenticationInterceptor;

    public WebMvcConfig(ApiWriteAuditInterceptor apiWriteAuditInterceptor,
                        RecentAuthenticationInterceptor recentAuthenticationInterceptor) {
        this.apiWriteAuditInterceptor = apiWriteAuditInterceptor;
        this.recentAuthenticationInterceptor = recentAuthenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Authentication endpoints are audited by AuthenticationAuditListener
        registry.addInterceptor(apiWriteAuditInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/auth/**");
        registry.addInterceptor(recentAuthenticationInterceptor).addPathPatterns("/v1/**");
    }
}
