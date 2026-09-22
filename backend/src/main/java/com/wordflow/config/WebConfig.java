package com.wordflow.config;

import com.wordflow.security.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置。
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** 静态资源目录 URI 的结尾符（resource location 必须以分隔符结尾） */
    private static final String RESOURCE_LOCATION_SUFFIX = "/";

    /** 头像等静态资源的访问前缀 */
    private static final String UPLOAD_URL_PATTERN = "/uploads/**";

    private final AuthInterceptor authInterceptor;
    @Value("${wordflow.upload-dir:uploads}")
    private String uploadDir;
    /** 允许跨域访问的来源（逗号分隔，生产通过 CORS_ALLOWED_ORIGINS 覆盖） */
    @Value("${wordflow.cors-allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String[] allowedOrigins;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith(RESOURCE_LOCATION_SUFFIX)) {
            location += RESOURCE_LOCATION_SUFFIX;
        }
        registry.addResourceHandler(UPLOAD_URL_PATTERN).addResourceLocations(location);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/error");
    }
}
