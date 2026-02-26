package com.example.demo.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON工具类
 * <p>
 * 基于Jackson封装的JSON序列化和反序列化工具方法。
 * 支持Java 8日期时间类型的处理。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
public final class JsonUtils {

    /** 共享的ObjectMapper实例，线程安全 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 静态初始化块：配置ObjectMapper
    static {
        // 注册Java 8日期时间模块
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 反序列化时忽略未知属性
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 允许单个值作为数组反序列化
        OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    /**
     * 私有构造函数，防止实例化
     */
    private JsonUtils() {
    }

    /**
     * 对象序列化为JSON字符串
     *
     * @param obj 要序列化的对象
     * @return JSON字符串，对象为null时返回null
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Object to JSON string error", e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * 对象序列化为格式化的JSON字符串
     *
     * @param obj 要序列化的对象
     * @return 格式化的JSON字符串，对象为null时返回null
     * @throws RuntimeException 序列化失败时抛出
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Object to pretty JSON string error", e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * JSON字符串反序列化为对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象，字符串为空时返回null
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON string to object error: {}", json, e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    /**
     * JSON字符串反序列化为复杂类型对象
     * <p>
     * 用于反序列化泛型集合等复杂类型
     * </p>
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           泛型类型
     * @return 反序列化后的对象，字符串为空时返回null
     * @throws RuntimeException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON string to object error: {}", json, e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    /**
     * 获取共享的ObjectMapper实例
     * <p>
     * 用于需要自定义序列化/反序列化配置的场景
     * </p>
     *
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}
