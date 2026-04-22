package me.xap3y.space.config;

import me.xap3y.space.interceptor.ApiKeyInterceptor;
import me.xap3y.space.interceptor.IpInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final IpInterceptor ipInterceptor;

    public WebConfig(ApiKeyInterceptor apiKeyInterceptor, IpInterceptor ipInterceptor) {
        this.apiKeyInterceptor = apiKeyInterceptor;
        this.ipInterceptor = ipInterceptor;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/v1/**");

        registry.addInterceptor(ipInterceptor)
                .addPathPatterns("/**");
    }
}
