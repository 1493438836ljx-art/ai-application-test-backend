package com.example.demo.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI文档配置类
 * <p>
 * 配置Swagger/OpenAPI 3.0文档，用于生成和展示RESTful API文档。
 * 访问地址：http://localhost:8080/swagger-ui.html
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * 创建OpenAPI文档配置
     * <p>
     * 配置API文档的基本信息，包括：
     * <ul>
     *   <li>API标题和描述</li>
     *   <li>版本号</li>
     *   <li>联系方式</li>
     *   <li>许可证信息</li>
     * </ul>
     * </p>
     *
     * @return OpenAPI文档对象
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("生成式AI应用推理测试平台 API")
                        .description("生成式AI应用/软件的推理特性测试平台RESTful API文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("AI Test Platform Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
