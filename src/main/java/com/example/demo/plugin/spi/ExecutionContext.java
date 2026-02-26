package com.example.demo.plugin.spi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 执行上下文类，封装执行插件运行时所需的上下文信息
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {

    /** 任务项ID，关联具体的测试任务项 */
    private Long taskItemId;

    /** 输入内容，待推理的文本或问题 */
    private String input;

    /** API端点地址，生成式AI服务的接口URL */
    private String apiEndpoint;

    /** 认证配置，包含认证类型和凭证信息 */
    private Map<String, Object> authConfig;

    /** 插件配置，执行插件的自定义参数 */
    private Map<String, Object> pluginConfig;
}
