package com.example.demo.agent.client;

import com.example.demo.agent.config.ClaudeCodeProperties;
import com.example.demo.agent.dto.StreamChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Claude Code 流式 API 客户端
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "claude.code", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClaudeCodeStreamClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ClaudeCodeProperties properties;

    public ClaudeCodeStreamClient(ClaudeCodeProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();

        // 配置 HttpClient 支持长连接
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(10))
                .compress(true);

        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        log.info("Claude Code 流式客户端初始化完成，基础URL: {}", properties.getBaseUrl());
    }

    /**
     * 流式执行任务
     */
    public Flux<StreamChunk> executeTaskStream(String taskContent, String configJson,
                                                byte[] skillFile, String skillFileName,
                                                String sessionId) {
        log.debug("开始流式任务，sessionId: {}, 任务摘要: {}",
                sessionId, taskContent != null && taskContent.length() > 50
                        ? taskContent.substring(0, 50) + "..." : taskContent);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("taskContent", taskContent);

        if (configJson != null && !configJson.isBlank()) {
            bodyBuilder.part("config", configJson);
        }

        if (skillFile != null && skillFile.length > 0 && skillFileName != null && !skillFileName.isBlank()) {
            ByteArrayResource resource = new ByteArrayResource(skillFile) {
                @Override
                public String getFilename() {
                    return skillFileName;
                }
            };
            bodyBuilder.part("skillFile", resource);
        }

        if (sessionId != null && !sessionId.isBlank()) {
            bodyBuilder.part("sessionId", sessionId);
        }

        log.info("发送流式请求到: {}/api/task/stream", properties.getBaseUrl());

        // 使用 String 类型接收 SSE 流
        return webClient.post()
                .uri("/api/task/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(s -> log.info("SSE 连接已建立"))
                .doOnCancel(() -> log.warn("SSE 连接被取消"))
                // 解析每一行
                .flatMap(content -> {
                    log.debug("收到原始内容: {}", content);
                    String[] lines = content.split("\n");
                    return Flux.fromArray(lines)
                            .filter(line -> line != null && !line.trim().isEmpty())
                            .map(this::parseSseLineToOptional)
                            .filter(Optional::isPresent)
                            .map(Optional::get);
                })
                .doOnNext(chunk -> log.info("收到流式块: type={}", chunk.getType()))
                .doOnError(e -> log.error("流式请求错误: {}", e.getMessage(), e))
                .doOnComplete(() -> log.info("流式任务完成"))
                .timeout(Duration.ofMinutes(10));
    }

    /**
     * 解析 SSE 行，返回 Optional
     * 支持两种格式：
     * 1. SSE 标准格式: "data: {...}"
     * 2. 纯 JSON 格式: "{...}" (WebClient bodyToFlux 可能已解析 SSE)
     */
    private Optional<StreamChunk> parseSseLineToOptional(String line) {
        try {
            if (line == null || line.trim().isEmpty()) {
                return Optional.empty();
            }

            String trimmedLine = line.trim();
            String jsonStr = null;

            if (trimmedLine.startsWith("data: ")) {
                jsonStr = trimmedLine.substring(6).trim();
            } else if (trimmedLine.startsWith("data:")) {
                jsonStr = trimmedLine.substring(5).trim();
            } else if (trimmedLine.startsWith("{")) {
                // 纯 JSON 格式（无 data: 前缀）
                jsonStr = trimmedLine;
            }

            if (jsonStr == null || jsonStr.isEmpty()) {
                return Optional.empty();
            }

            StreamChunk chunk = parseChunk(jsonStr);
            return Optional.ofNullable(chunk);
        } catch (Exception e) {
            log.warn("解析 SSE 行失败: {}, 错误: {}", line, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 解析 JSON 数据为 StreamChunk
     */
    private StreamChunk parseChunk(String dataStr) {
        try {
            return objectMapper.readValue(dataStr, StreamChunk.class);
        } catch (Exception e) {
            log.warn("解析 JSON 失败: {}, 错误: {}", dataStr, e.getMessage());
            // 解析失败时，创建一个包含原始数据的 chunk
            StreamChunk fallbackChunk = new StreamChunk();
            fallbackChunk.setType("chunk");
            fallbackChunk.setContent(dataStr);
            return fallbackChunk;
        }
    }

    /**
     * 流式执行任务（简化版）
     */
    public Flux<StreamChunk> executeTaskStream(String taskContent, String sessionId) {
        return executeTaskStream(taskContent, null, null, null, sessionId);
    }
}
