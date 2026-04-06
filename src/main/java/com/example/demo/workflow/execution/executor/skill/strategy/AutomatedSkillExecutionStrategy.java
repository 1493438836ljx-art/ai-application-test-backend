package com.example.demo.workflow.execution.executor.skill.strategy;

import com.example.demo.workflow.execution.executor.skill.dto.SkillExecutionRequest;
import com.example.demo.workflow.execution.executor.skill.dto.SkillExecutionResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 自动化脚本执行策略
 * 执行zip压缩的执行套件
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AutomatedSkillExecutionStrategy implements SkillExecutionStrategy {

    private static final Pattern OUTPUT_PATTERN = Pattern.compile("^(\\w+)\\s*:\\s*(.+)$");
    private static final long DEFAULT_TIMEOUT_MS = 60000; // 60秒超时

    @Override
    public String getExecutionType() {
        return "AUTOMATED";
    }

    @Override
    public String getStrategyName() {
        return "自动化脚本执行策略";
    }

    @Override
    public SkillExecutionResult execute(SkillExecutionRequest request) {
        log.info("执行自动化脚本: skillId={}, skillName={}",
                request.getSkillId(), request.getSkillName());

        long startTime = System.currentTimeMillis();
        Path workDir = null;

        try {
            // 1. 验证套件内容
            if (request.getSuiteContent() == null || request.getSuiteContent().length == 0) {
                return SkillExecutionResult.failure("执行套件内容为空");
            }

            // 2. 创建临时工作目录
            workDir = Files.createTempDirectory("skill_exec_" + request.getSkillId());
            log.debug("创建临时工作目录: {}", workDir);

            // 3. 解压执行套件
            List<String> scriptFiles = unzipSuite(request.getSuiteContent(), workDir);
            if (scriptFiles.isEmpty()) {
                return SkillExecutionResult.failure("执行套件中没有找到脚本文件");
            }

            // 4. 查找入口脚本（优先查找main.py或同名py文件）
            String entryScript = findEntryScript(scriptFiles, request.getSkillName());
            if (entryScript == null) {
                return SkillExecutionResult.failure("未找到可执行的Python脚本");
            }
            log.info("找到入口脚本: {}", entryScript);

            // 5. 构建命令行参数
            List<String> commandArgs = buildCommandArgs(request.getInputParameters());

            // 6. 执行脚本
            ScriptExecutionResult execResult = executeScript(
                    workDir.resolve(entryScript),
                    commandArgs,
                    request.getTimeoutMs() != null && request.getTimeoutMs() > 0 ?
                        request.getTimeoutMs() : DEFAULT_TIMEOUT_MS
            );

            if (!execResult.isSuccess()) {
                return SkillExecutionResult.failure("脚本执行失败: " + execResult.getErrorMessage());
            }

            // 7. 解析输出结果
            Map<String, Object> outputs = parseOutputs(execResult.getOutput());
            log.info("解析输出结果: {}", outputs);

            long durationMs = System.currentTimeMillis() - startTime;

            return SkillExecutionResult.success(outputs, execResult.getOutput(), durationMs);

        } catch (Exception e) {
            log.error("执行自动化脚本异常: skillId={}", request.getSkillId(), e);
            return SkillExecutionResult.failure("执行失败: " + e.getMessage());
        } finally {
            // 8. 清理临时目录
            if (workDir != null) {
                cleanupWorkDir(workDir);
            }
        }
    }

    /**
     * 解压执行套件
     */
    private List<String> unzipSuite(byte[] suiteContent, Path targetDir) throws IOException {
        List<String> extractedFiles = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(suiteContent))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());

                // 安全检查：防止zip slip攻击
                if (!entryPath.normalize().startsWith(targetDir.normalize())) {
                    log.warn("跳过不安全的zip条目: {}", entry.getName());
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    extractedFiles.add(entry.getName());
                    log.debug("解压文件: {}", entry.getName());
                }
            }
        }

        return extractedFiles;
    }

    /**
     * 查找入口脚本
     */
    private String findEntryScript(List<String> scriptFiles, String skillName) {
        // 优先查找main.py
        for (String file : scriptFiles) {
            if (file.endsWith("main.py") || file.equals("main.py")) {
                return file;
            }
        }

        // 查找与skill名称匹配的py文件
        if (skillName != null) {
            String skillNameLower = skillName.toLowerCase().replaceAll("\\s+", "_");
            for (String file : scriptFiles) {
                if (file.toLowerCase().endsWith(".py")) {
                    String fileName = new File(file).getName();
                    if (fileName.toLowerCase().contains(skillNameLower)) {
                        return file;
                    }
                }
            }
        }

        // 返回第一个.py文件
        for (String file : scriptFiles) {
            if (file.toLowerCase().endsWith(".py")) {
                return file;
            }
        }

        return null;
    }

    /**
     * 构建命令行参数
     */
    private List<String> buildCommandArgs(List<SkillExecutionRequest.SkillParameterDef> parameters) {
        List<String> args = new ArrayList<>();
        if (parameters != null) {
            for (SkillExecutionRequest.SkillParameterDef param : parameters) {
                Object value = param.getValue();
                if (value != null) {
                    args.add(value.toString());
                }
            }
        }
        return args;
    }

    /**
     * 执行Python脚本
     */
    private ScriptExecutionResult executeScript(Path scriptPath, List<String> args, long timeoutMs) {
        try {
            // 构建完整命令 - 优先使用完整路径
            List<String> command = new ArrayList<>();
            String pythonPath = System.getenv("PYTHON_PATH");
            if (pythonPath != null && !pythonPath.isEmpty()) {
                command.add(pythonPath);
            } else {
                // Windows默认路径
                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("win")) {
                    command.add("C:\\Users\\Administrator\\AppData\\Local\\Programs\\Python\\Python313\\python.exe");
                } else {
                    command.add("python3");
                }
            }
            command.add(scriptPath.getFileName().toString());
            command.addAll(args);

            log.debug("执行命令: {} in directory: {}", String.join(" ", command), scriptPath.getParent());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(scriptPath.getParent().toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待执行完成
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ScriptExecutionResult.failure("脚本执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return ScriptExecutionResult.failure(
                        "脚本执行失败，退出码: " + exitCode + ", 输出: " + output
                );
            }

            return ScriptExecutionResult.success(output.toString());

        } catch (Exception e) {
            return ScriptExecutionResult.failure("执行脚本异常: " + e.getMessage());
        }
    }

    /**
     * 解析输出结果
     * 格式: key: value 或 sum: 30
     */
    private Map<String, Object> parseOutputs(String output) {
        Map<String, Object> result = new HashMap<>();

        if (output == null || output.isEmpty()) {
            return result;
        }

        String[] lines = output.split("\n");
        for (String line : lines) {
                Matcher matcher = OUTPUT_PATTERN.matcher(line.trim());
                if (matcher.matches()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    // 尝试转换为数字
                    try {
                        result.put(key, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        result.put(key, value);
                    }
                }
            }

        return result;
    }

    /**
     * 清理临时目录
     */
    private void cleanupWorkDir(Path workDir) {
        try {
            Files.walk(workDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.trace("删除文件失败: {}", path);
                        }
                    });
            Files.deleteIfExists(workDir);
            log.debug("清理临时目录: {}", workDir);
        } catch (Exception e) {
            log.warn("清理临时目录失败: {}", workDir, e);
        }
    }

    /**
     * 脚本执行内部结果
     */
    @Data
    @AllArgsConstructor
    private static class ScriptExecutionResult {
        private boolean success;
        private String output;
        private String errorMessage;

        public static ScriptExecutionResult success(String output) {
            return new ScriptExecutionResult(true, output, null);
        }

        public static ScriptExecutionResult failure(String errorMessage) {
            return new ScriptExecutionResult(false, null, errorMessage);
        }
    }
}
