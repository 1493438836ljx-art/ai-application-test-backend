package com.example.demo.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC配置类
 * <p>
 * 配置Web层的相关设置，主要包括跨域资源共享(CORS)配置，
 * 允许前端应用跨域访问后端API。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置CORS跨域映射
     * <p>
     * 允许所有来源的请求访问/api/**路径下的接口，支持以下HTTP方法：
     * GET、POST、PUT、PATCH、DELETE、OPTIONS
     * </p>
     *
     * @param registry CORS注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")              // 对/api/**路径启用CORS
                .allowedOriginPatterns("*")          // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")  // 允许的HTTP方法
                .allowedHeaders("*")                 // 允许所有请求头
                .allowCredentials(true)              // 允许携带凭证（Cookie等）
                .maxAge(3600);                       // 预检请求缓存时间（秒）
    }
}
