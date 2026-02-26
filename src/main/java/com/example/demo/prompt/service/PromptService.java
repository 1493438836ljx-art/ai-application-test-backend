package com.example.demo.prompt.service;

import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.exception.ErrorCode;
import com.example.demo.prompt.dto.*;
import com.example.demo.prompt.entity.Prompt;
import com.example.demo.prompt.mapper.PromptMapper;
import com.example.demo.prompt.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt服务类
 * <p>
 * 提供Prompt模板的业务逻辑处理，包括CRUD操作和模板渲染功能。
 * 模板变量使用{{variableName}}格式。
 * </p>
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    /** 变量占位符正则表达式：匹配{{variableName}}格式 */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final PromptRepository promptRepository;
    private final PromptMapper promptMapper;

    /**
     * 创建Prompt模板
     *
     * @param request 创建请求
     * @return 创建后的Prompt响应
     */
    @Transactional
    public PromptResponse createPrompt(PromptCreateRequest request) {
        Prompt prompt = promptMapper.toEntity(request);
        // 自动提取模板中的变量名
        prompt.setVariableNames(extractVariableNames(request.getTemplate()));
        Prompt saved = promptRepository.save(prompt);
        return promptMapper.toResponse(saved);
    }

    /**
     * 分页查询Prompt列表
     *
     * @param name     名称过滤条件（可选）
     * @param pageable 分页参数
     * @return Prompt分页结果
     */
    @Transactional(readOnly = true)
    public Page<PromptResponse> getPrompts(String name, Pageable pageable) {
        Page<Prompt> prompts;
        if (StringUtils.isNotBlank(name)) {
            prompts = promptRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            prompts = promptRepository.findAll(pageable);
        }
        return prompts.map(promptMapper::toResponse);
    }

    /**
     * 根据ID获取Prompt详情
     *
     * @param id Prompt ID
     * @return Prompt响应
     */
    @Transactional(readOnly = true)
    public PromptResponse getPromptById(Long id) {
        Prompt prompt = findPromptById(id);
        return promptMapper.toResponse(prompt);
    }

    /**
     * 根据名称获取Prompt详情
     *
     * @param name Prompt名称
     * @return Prompt响应
     */
    @Transactional(readOnly = true)
    public PromptResponse getPromptByName(String name) {
        Prompt prompt = promptRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_NOT_FOUND, "Prompt不存在: " + name));
        return promptMapper.toResponse(prompt);
    }

    /**
     * 更新Prompt模板
     *
     * @param id      Prompt ID
     * @param request 更新请求
     * @return 更新后的Prompt响应
     */
    @Transactional
    public PromptResponse updatePrompt(Long id, PromptUpdateRequest request) {
        Prompt prompt = findPromptById(id);
        promptMapper.updateEntity(request, prompt);

        // 如果模板内容有更新，重新提取变量名
        if (request.getTemplate() != null) {
            prompt.setVariableNames(extractVariableNames(request.getTemplate()));
        }

        Prompt updated = promptRepository.save(prompt);
        return promptMapper.toResponse(updated);
    }

    /**
     * 删除Prompt模板
     *
     * @param id Prompt ID
     */
    @Transactional
    public void deletePrompt(Long id) {
        if (!promptRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PROMPT_NOT_FOUND);
        }
        promptRepository.deleteById(id);
    }

    /**
     * 根据ID渲染Prompt模板
     *
     * @param id      Prompt ID
     * @param request 渲染请求（包含变量值）
     * @return 渲染结果
     */
    public PromptRenderResponse renderPrompt(Long id, PromptRenderRequest request) {
        Prompt prompt = findPromptById(id);
        return renderTemplate(prompt.getTemplate(), prompt.getVariableNames(), request.getVariables());
    }

    /**
     * 直接渲染模板字符串
     *
     * @param template  模板内容
     * @param variables 变量值映射
     * @return 渲染结果
     */
    public PromptRenderResponse renderTemplate(String template, Map<String, Object> variables) {
        String variableNames = extractVariableNames(template);
        return renderTemplate(template, variableNames, variables);
    }

    /**
     * 渲染模板的内部实现
     *
     * @param template         模板内容
     * @param variableNamesStr 变量名字符串
     * @param variables        变量值映射
     * @return 渲染结果
     */
    private PromptRenderResponse renderTemplate(String template, String variableNamesStr, Map<String, Object> variables) {
        if (variables == null) {
            variables = Collections.emptyMap();
        }

        List<String> variableList = parseVariableNames(variableNamesStr);
        List<String> missingVariables = new ArrayList<>();
        String renderedContent = template;

        // 逐个替换变量占位符
        for (String varName : variableList) {
            Object value = variables.get(varName);
            if (value == null) {
                // 记录缺失的变量
                missingVariables.add(varName);
                value = "";
            }
            renderedContent = renderedContent.replace("{{" + varName + "}}", String.valueOf(value));
        }

        return PromptRenderResponse.builder()
                .renderedContent(renderedContent)
                .missingVariables(missingVariables)
                .build();
    }

    /**
     * 根据ID查找Prompt
     *
     * @param id Prompt ID
     * @return Prompt实体
     * @throws BusinessException Prompt不存在时抛出
     */
    private Prompt findPromptById(Long id) {
        return promptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_NOT_FOUND));
    }

    /**
     * 从模板中提取变量名
     *
     * @param template 模板内容
     * @return 变量名字符串（逗号分隔）
     */
    private String extractVariableNames(String template) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return String.join(",", variables);
    }

    /**
     * 解析变量名字符串为列表
     *
     * @param variableNames 变量名字符串
     * @return 变量名列表
     */
    private List<String> parseVariableNames(String variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(variableNames.split(","));
    }
}
