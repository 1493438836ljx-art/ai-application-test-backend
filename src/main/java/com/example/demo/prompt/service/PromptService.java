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

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final PromptRepository promptRepository;
    private final PromptMapper promptMapper;

    @Transactional
    public PromptResponse createPrompt(PromptCreateRequest request) {
        Prompt prompt = promptMapper.toEntity(request);
        prompt.setVariableNames(extractVariableNames(request.getTemplate()));
        Prompt saved = promptRepository.save(prompt);
        return promptMapper.toResponse(saved);
    }

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

    @Transactional(readOnly = true)
    public PromptResponse getPromptById(Long id) {
        Prompt prompt = findPromptById(id);
        return promptMapper.toResponse(prompt);
    }

    @Transactional(readOnly = true)
    public PromptResponse getPromptByName(String name) {
        Prompt prompt = promptRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_NOT_FOUND, "Prompt不存在: " + name));
        return promptMapper.toResponse(prompt);
    }

    @Transactional
    public PromptResponse updatePrompt(Long id, PromptUpdateRequest request) {
        Prompt prompt = findPromptById(id);
        promptMapper.updateEntity(request, prompt);

        if (request.getTemplate() != null) {
            prompt.setVariableNames(extractVariableNames(request.getTemplate()));
        }

        Prompt updated = promptRepository.save(prompt);
        return promptMapper.toResponse(updated);
    }

    @Transactional
    public void deletePrompt(Long id) {
        if (!promptRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PROMPT_NOT_FOUND);
        }
        promptRepository.deleteById(id);
    }

    public PromptRenderResponse renderPrompt(Long id, PromptRenderRequest request) {
        Prompt prompt = findPromptById(id);
        return renderTemplate(prompt.getTemplate(), prompt.getVariableNames(), request.getVariables());
    }

    public PromptRenderResponse renderTemplate(String template, Map<String, Object> variables) {
        String variableNames = extractVariableNames(template);
        return renderTemplate(template, variableNames, variables);
    }

    private PromptRenderResponse renderTemplate(String template, String variableNamesStr, Map<String, Object> variables) {
        if (variables == null) {
            variables = Collections.emptyMap();
        }

        List<String> variableList = parseVariableNames(variableNamesStr);
        List<String> missingVariables = new ArrayList<>();
        String renderedContent = template;

        for (String varName : variableList) {
            Object value = variables.get(varName);
            if (value == null) {
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

    private Prompt findPromptById(Long id) {
        return promptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_NOT_FOUND));
    }

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

    private List<String> parseVariableNames(String variableNames) {
        if (variableNames == null || variableNames.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(variableNames.split(","));
    }
}
