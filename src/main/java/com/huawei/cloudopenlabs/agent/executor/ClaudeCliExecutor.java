/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.executor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.agent.config.ClaudeCodeProperties;
import com.huawei.cloudopenlabs.agent.dto.StreamChunk;
import com.huawei.cloudopenlabs.agent.exception.ClaudeCliException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Claude CLI 执行器
 * <p>
 * 直接调用 Claude CLI，替代原有的 Node.js 中间层
 * </p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>使用 ProcessBuilder 直接调用 Claude CLI</li>
 *   <li>流式输出解析（stream-json 格式）</li>
 *   <li>支持会话持久化（--session-id / --resume）</li>
 *   <li>支持 Skill 目录（--add-dir）</li>
 * </ul>
 *
 * @author GNEEC LIVE
 * @version 27.0.2.0
 * @since 2026-04-13
 */
@Slf4j
@Component
public class ClaudeCliExecutor {

    private final ClaudeCodeProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService streamReaderExecutor;

    /**
     * 默认执行超时时间：5分钟
     */
    private static final long DEFAULT_TIMEOUT_MS = 300000;

    /**
     * Claude CLI 可执行文件名
     */
    private static final String CLAUDE_CLI = "claude";

    public ClaudeCliExecutor(@Autowired ClaudeCodeProperties properties) {
        this.properties = properties;
        this.objectMapper = createObjectMapper();

        // 创建专用线程池用于读取流
        int readerThreads = 4;
        this.streamReaderExecutor = Executors.newFixedThreadPool(readerThreads, r -> {
            Thread t = new Thread(r, "claude-cli-stream-reader");
            t.setDaemon(true);
            return t;
        });

        log.info("ClaudeCliExecutor initialized, new architecture mode: {}", properties.getUseNewArchitecture());
    }

    /**
     * 创建配置好的 ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper;
    }

    /**
     * 执行 Claude CLI 任务（流式）
     *
     * @param request       执行请求
     * @param chunkConsumer 流式数据消费者
     * @param onComplete    完成回调
     * @param onError       错误回调
     * @return 执行结果
     */
    public ClaudeExecutionResult executeStream(
            ClaudeExecutionRequest request,
            Consumer<StreamChunk> chunkConsumer,
            Runnable onComplete,
            Consumer<Throwable> onError) {

        // 构建命令参数
        List<String> command = buildCommand(request);
        log.info("Executing Claude CLI: {}", maskSensitiveArgs(command));

        // 创建进程构建器
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(System.getProperty("user.dir")));

        // 复制环境变量
        Map<String, String> env = pb.environment();
        if (request.getEnvironment() != null) {
            env.putAll(request.getEnvironment());
        }

        // 不合并错误流，分别处理
        pb.redirectErrorStream(false);

        Process process = null;
        try {
            process = pb.start();
            final Process finalProcess = process;

            // 用于跟踪是否已完成
            AtomicReference<Boolean> completed = new AtomicReference<>(false);
            AtomicReference<String> sessionId = new AtomicReference<>(request.getSessionId());

            // 发送开始事件
            if (chunkConsumer != null) {
                StreamChunk startChunk = new StreamChunk();
                startChunk.setType("start");
                startChunk.setSessionId(request.getSessionId());
                chunkConsumer.accept(startChunk);
            }

            // 异步读取标准输出
            Future<?> stdoutFuture = streamReaderExecutor.submit(() -> {
                readOutputStream(finalProcess.getInputStream(), chunkConsumer, sessionId, completed);
            });

            // 异步读取标准错误
            Future<?> stderrFuture = streamReaderExecutor.submit(() -> {
                readErrorStream(finalProcess.getErrorStream());
            });

            // 写入输入（如果有）
            if (request.getInput() != null && !request.getInput().isEmpty()) {
                try (OutputStream stdin = finalProcess.getOutputStream()) {
                    stdin.write(request.getInput().getBytes(StandardCharsets.UTF_8));
                    stdin.flush();
                } catch (IOException e) {
                    log.warn("Failed to write stdin (process may have ended): {}", e.getMessage());
                }
            }

            // 等待进程完成
            long timeout = DEFAULT_TIMEOUT_MS;
            boolean finished = finalProcess.waitFor(timeout, TimeUnit.MILLISECONDS);

            if (!finished) {
                finalProcess.destroyForcibly();
                String errorMsg = "Claude CLI 执行超时（" + (timeout / 1000) + "秒）";
                log.error(errorMsg);
                throw new ClaudeCliException(errorMsg);
            }

            // 等待流读取完成（最多等待 10 秒）
            try {
                stdoutFuture.get(10, TimeUnit.SECONDS);
                stderrFuture.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("Stream read timeout, continuing processing");
            }

            int exitCode = finalProcess.exitValue();

            if (exitCode != 0) {
                String errorMsg = "Claude CLI 退出码异常: " + exitCode;
                log.error(errorMsg);
                throw new ClaudeCliException(errorMsg, exitCode);
            }

            // 标记完成
            completed.set(true);

            // 发送完成事件
            if (chunkConsumer != null) {
                StreamChunk doneChunk = new StreamChunk();
                doneChunk.setType("done");
                doneChunk.setSessionId(sessionId.get());
                doneChunk.setDuration(0L); // 由调用方设置实际耗时
                chunkConsumer.accept(doneChunk);
            }

            if (onComplete != null) {
                onComplete.run();
            }

            return new ClaudeExecutionResult(sessionId.get(), exitCode, 0L);

        } catch (IOException e) {
            log.error("Claude CLI IO exception", e);
            if (onError != null) {
                onError.accept(e);
            }
            throw new ClaudeCliException("执行 Claude CLI 失败: " + e.getMessage(), e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new ClaudeCliException("执行被中断", e);

        } catch (ExecutionException e) {
            log.error("Output stream read exception", e);
            if (onError != null) {
                onError.accept(e);
            }
            throw new ClaudeCliException("Output stream read exception: " + e.getCause().getMessage(), e.getCause());
        }
    }

    /**
     * 构建命令参数
     */
    private List<String> buildCommand(ClaudeExecutionRequest request) {
        List<String> args = new ArrayList<>();

        // 基础命令
        args.add(CLAUDE_CLI);
        args.add("code");
        args.add("-p");  // 非交互模式

        // 输出格式
        args.add("--output-format");
        args.add("stream-json");

        // 详细模式
        if (Boolean.TRUE.equals(properties.getShowDangerModeWarning())) {
            args.add("--verbose");
        }

        // 危险模式（跳过权限检查）
        // 注意：生产环境应谨慎使用
        args.add("--dangerously-skip-permissions");

        // 会话管理
        String sessionId = request.getSessionId();
        if (sessionId != null && !sessionId.isEmpty()) {
            if (request.isResume()) {
                args.add("--resume");
            } else {
                args.add("--session-id");
            }
            args.add(sessionId);
        }

        // Skill 目录
        String skillDir = request.getSkillDir();
        if (skillDir != null && !skillDir.isEmpty()) {
            args.add("--add-dir");
            args.add(skillDir);
        }

        return args;
    }

    /**
     * 读取输出流并解析
     */
    private void readOutputStream(
            InputStream inputStream,
            Consumer<StreamChunk> chunkConsumer,
            AtomicReference<String> sessionId,
            AtomicReference<Boolean> completed) {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    StreamChunk chunk = parseStreamLine(line);

                    if (chunk != null) {
                        // 更新 sessionId
                        if (chunk.getSessionId() != null && !chunk.getSessionId().isEmpty()) {
                            sessionId.set(chunk.getSessionId());
                        }

                        if (chunkConsumer != null && !completed.get()) {
                            chunkConsumer.accept(chunk);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse stream data, treating as raw text: {} ...",
                            line.substring(0, Math.min(50, line.length())));

                    // 作为原始文本处理
                    if (chunkConsumer != null && !completed.get()) {
                        StreamChunk textChunk = new StreamChunk();
                        textChunk.setType("chunk");
                        textChunk.setContentType("text");
                        textChunk.setContent(line);
                        chunkConsumer.accept(textChunk);
                    }
                }
            }
        } catch (IOException e) {
            if (!completed.get()) {
                log.error("Output stream read exception", e);
            }
        }
    }

    /**
     * 读取错误流
     */
    private void readErrorStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 只记录警告级别以上的错误
                if (line.contains("error") || line.contains("Error") || line.contains("ERROR")) {
                    log.error("Claude CLI stderr: {}", line);
                } else {
                    log.debug("Claude CLI stderr: {}", line);
                }
            }
        } catch (IOException e) {
            log.debug("Error stream read exception (process may have ended): {}", e.getMessage());
        }
    }

    /**
     * 解析流数据行
     * <p>
     * Claude CLI stream-json 输出格式示例：
     * <pre>
     * {"type":"message","content":"...","role":"assistant"}
     * {"type":"tool_use","name":"...","input":{...}}
     * {"type":"result","content":"..."}
     * </pre>
     * </p>
     */
    private StreamChunk parseStreamLine(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(line);
            String type = getTextOrDefault(root, "type", "chunk");

            StreamChunk chunk = new StreamChunk();
            chunk.setType(mapType(type));

            // 根据类型解析不同字段
            switch (type.toLowerCase()) {
                case "system":
                case "message":
                    // 会话开始或消息
                    chunk.setContentType("text");
                    chunk.setContent(getTextOrDefault(root, "content", ""));
                    String sid = getTextOrDefault(root, "session_id", null);
                    if (sid == null) {
                        sid = getTextOrDefault(root, "sessionId", null);
                    }
                    chunk.setSessionId(sid);
                    break;

                case "assistant":
                case "content_block":
                    // AI 输出
                    chunk.setContentType(mapContentType(getTextOrDefault(root, "content_type", "text")));
                    chunk.setContent(extractContent(root));
                    break;

                case "tool_use":
                    // 工具使用
                    chunk.setContentType("tool_use");
                    chunk.setToolName(getTextOrDefault(root, "name", ""));
                    if (root.has("input")) {
                        chunk.setToolInput(root.get("input"));
                    }
                    break;

                case "tool_result":
                case "result":
                    // 工具结果
                    chunk.setContentType("result");
                    chunk.setContent(getTextOrDefault(root, "content", ""));
                    break;

                case "thinking":
                    // 思考过程
                    chunk.setContentType("thinking");
                    chunk.setContent(getTextOrDefault(root, "thinking", ""));
                    break;

                case "error":
                    // 错误
                    chunk.setContent(getTextOrDefault(root, "message",
                            getTextOrDefault(root, "error", "Unknown error")));
                    break;

                default:
                    // 默认处理
                    chunk.setContentType("text");
                    chunk.setContent(getTextOrDefault(root, "content", line));
            }

            return chunk;

        } catch (Exception e) {
            log.debug("JSON parsing failed, returning raw text: {}", e.getMessage());
            StreamChunk chunk = new StreamChunk();
            chunk.setType("chunk");
            chunk.setContentType("text");
            chunk.setContent(line);
            return chunk;
        }
    }

    /**
     * 提取内容
     */
    private String extractContent(JsonNode root) {
        if (root.has("content")) {
            JsonNode contentNode = root.get("content");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            } else if (contentNode.isArray()) {
                // 处理内容数组
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : contentNode) {
                    if (item.isTextual()) {
                        sb.append(item.asText());
                    } else if (item.has("text")) {
                        sb.append(item.get("text").asText());
                    }
                }
                return sb.toString();
            }
            return contentNode.toString();
        }
        return "";
    }

    /**
     * 映射类型
     */
    private String mapType(String type) {
        if (type == null) {
            return "chunk";
        }
        switch (type.toLowerCase()) {
            case "system":
            case "init":
                return "start";
            case "message":
            case "assistant":
            case "content_block":
            case "tool_use":
            case "tool_result":
            case "result":
            case "thinking":
                return "chunk";
            case "done":
            case "complete":
                return "done";
            case "error":
                return "error";
            default:
                return "chunk";
        }
    }

    /**
     * 映射内容类型
     */
    private String mapContentType(String contentType) {
        if (contentType == null) {
            return "text";
        }
        switch (contentType.toLowerCase()) {
            case "thinking":
            case "thought":
                return "thinking";
            case "tool_use":
            case "tool":
                return "tool_use";
            case "result":
            case "tool_result":
                return "result";
            default:
                return "text";
        }
    }

    /**
     * 获取文本字段，支持默认值
     */
    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        if (node.has(field)) {
            JsonNode fieldNode = node.get(field);
            if (fieldNode.isTextual()) {
                return fieldNode.asText();
            } else if (fieldNode.isNull()) {
                return defaultValue;
            }
            return fieldNode.toString();
        }
        return defaultValue;
    }

    /**
     * 屏蔽敏感参数（用于日志）
     */
    private String maskSensitiveArgs(List<String> args) {
        List<String> masked = new ArrayList<>();
        boolean nextIsSensitive = false;

        for (String arg : args) {
            if (nextIsSensitive) {
                // 屏蔽敏感值
                masked.add(arg.length() > 8 ?
                        arg.substring(0, 4) + "****" + arg.substring(arg.length() - 4) :
                        "****");
                nextIsSensitive = false;
            } else if (arg.equals("--session-id") || arg.equals("--resume") || arg.equals("--add-dir")) {
                masked.add(arg);
                nextIsSensitive = true;
            } else {
                masked.add(arg);
            }
        }

        return String.join(" ", masked);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ClaudeCliExecutor thread pool");
        streamReaderExecutor.shutdown();
        try {
            if (!streamReaderExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                streamReaderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            streamReaderExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
