package com.example.demo.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ消息队列配置类
 * <p>
 * 配置RabbitMQ用于异步处理测试任务。包含任务队列和死信队列的配置，
 * 支持任务的消息持久化、失败重试和死信处理。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 任务队列相关常量 ====================

    /** 任务队列名称 */
    public static final String TASK_QUEUE = "task.queue";
    /** 任务交换机名称 */
    public static final String TASK_EXCHANGE = "task.exchange";
    /** 任务路由键 */
    public static final String TASK_ROUTING_KEY = "task.routing.key";

    // ==================== 死信队列相关常量 ====================

    /** 死信队列名称 - 用于存储处理失败的消息 */
    public static final String DEAD_LETTER_QUEUE = "task.dlq";
    /** 死信交换机名称 */
    public static final String DEAD_LETTER_EXCHANGE = "task.dlx";
    /** 死信路由键 */
    public static final String DEAD_LETTER_ROUTING_KEY = "task.dlq.routing.key";

    /**
     * 创建JSON消息转换器
     * <p>
     * 使用Jackson将消息对象序列化为JSON格式进行传输
     * </p>
     *
     * @return JSON消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建RabbitMQ模板
     *
     * @param connectionFactory RabbitMQ连接工厂
     * @return 配置好的RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * 配置消息监听容器工厂
     * <p>
     * 配置说明：
     * <ul>
     *   <li>并发消费者数：3-10个</li>
     *   <li>预取数量：1（保证任务公平分发）</li>
     * </ul>
     * </p>
     *
     * @param connectionFactory RabbitMQ连接工厂
     * @return 配置好的监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);          // 初始并发消费者数
        factory.setMaxConcurrentConsumers(10);      // 最大并发消费者数
        factory.setPrefetchCount(1);                // 每次只取一条消息，确保公平分发
        return factory;
    }

    /**
     * 创建任务交换机
     *
     * @return 直连型交换机
     */
    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE);
    }

    /**
     * 创建死信交换机
     *
     * @return 直连型交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    /**
     * 创建任务队列
     * <p>
     * 队列特性：
     * <ul>
     *   <li>持久化：重启后队列不丢失</li>
     *   <li>死信配置：消息被拒绝或过期时转发到死信队列</li>
     * </ul>
     * </p>
     *
     * @return 任务队列
     */
    @Bean
    public Queue taskQueue() {
        return QueueBuilder.durable(TASK_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    /**
     * 创建死信队列
     * <p>
     * 用于存储处理失败的消息，便于后续排查和重试
     * </p>
     *
     * @return 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    /**
     * 绑定任务队列到任务交换机
     *
     * @param taskQueue    任务队列
     * @param taskExchange 任务交换机
     * @return 绑定关系
     */
    @Bean
    public Binding taskBinding(Queue taskQueue, DirectExchange taskExchange) {
        return BindingBuilder.bind(taskQueue).to(taskExchange).with(TASK_ROUTING_KEY);
    }

    /**
     * 绑定死信队列到死信交换机
     *
     * @param deadLetterQueue    死信队列
     * @param deadLetterExchange 死信交换机
     * @return 绑定关系
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }
}
