package com.example.demo.plugin.spi;

/**
 * 执行插件接口，定义测试推理执行的标准行为
 * <p>
 * 所有执行插件都需要实现此接口，用于调用生成式AI服务进行推理测试
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface ExecutionPlugin {

    /**
     * 获取插件名称
     *
     * @return 插件的唯一标识名称
     */
    String getName();

    /**
     * 获取插件描述
     *
     * @return 插件的功能描述信息
     */
    String getDescription();

    /**
     * 执行推理测试
     *
     * @param context 执行上下文，包含执行所需的所有参数
     * @return 执行结果，包含输出内容和执行状态
     */
    ExecutionResult execute(ExecutionContext context);
}
