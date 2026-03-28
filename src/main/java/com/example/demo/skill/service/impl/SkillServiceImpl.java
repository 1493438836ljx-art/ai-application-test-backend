package com.example.demo.skill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.skill.dto.*;
import com.example.demo.skill.entity.*;
import com.example.demo.skill.mapper.*;
import com.example.demo.skill.service.SkillService;
import com.example.demo.skill.util.ParameterTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Skill服务实现类
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillMapper skillMapper;
    private final SkillParameterMapper parameterMapper;
    private final SkillAccessControlMapper accessControlMapper;

    @Value("${skill.suite.upload-path:./uploads/suites}")
    private String uploadPath;

    @Override
    @Transactional
    public SkillResponse createSkill(SkillCreateRequest request, MultipartFile file) {
        log.info("创建Skill: {}", request.getName());

        // 检查名称是否已存在
        Long count = skillMapper.countByName(request.getName());
        if (count > 0) {
            throw BusinessException.conflict("Skill名称已存在: " + request.getName());
        }

        // 处理文件上传
        String suitePath = null;
        String suiteFilename = null;
        if (file != null && !file.isEmpty()) {
            suitePath = saveSuiteFile(file);
            suiteFilename = file.getOriginalFilename();
        }

        // 设置默认创建人
        String createdBy = request.getCreatedBy();
        if (createdBy == null || createdBy.isEmpty()) {
            createdBy = "system";
        }

        // 创建Skill主表记录
        SkillEntity skill = new SkillEntity();
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setSuitePath(suitePath);
        skill.setSuiteFilename(suiteFilename);
        skill.setExecutionType(request.getExecutionType());
        skill.setCategory(request.getCategory());
        skill.setAccessType(request.getAccessType());
        skill.setIsContainer(request.getIsContainer());
        skill.setAllowAddInputParams(request.getAllowAddInputParams() != null ? request.getAllowAddInputParams() : false);
        skill.setAllowAddOutputParams(request.getAllowAddOutputParams() != null ? request.getAllowAddOutputParams() : false);
        skill.setStatus(SkillStatus.DRAFT.name());
        skill.setCreatedBy(createdBy);
        skill.setUpdatedBy(createdBy);
        skill.setDeleted(false);

        skillMapper.insert(skill);
        String skillId = skill.getId();

        // 保存入参
        if (request.getInputParameters() != null && !request.getInputParameters().isEmpty()) {
            int order = 1;
            for (SkillCreateRequest.InputParameterDTO paramDTO : request.getInputParameters()) {
                // 验证参数类型
                if (paramDTO.getParamType() != null && !paramDTO.getParamType().isEmpty()) {
                    ParameterTypeValidator.validateType(paramDTO.getParamType(), paramDTO.getParamName());
                }
                SkillParameterEntity param = new SkillParameterEntity();
                param.setSkillId(skillId);
                param.setParamDirection(SkillParamDirection.INPUT.name());
                param.setParamOrder(order++);
                param.setParamType(paramDTO.getParamType());
                param.setParamName(paramDTO.getParamName());
                param.setDefaultValue(paramDTO.getDefaultValue());
                param.setDescription(paramDTO.getDescription());
                param.setRequired(paramDTO.getRequired());
                parameterMapper.insert(param);
            }
        }

        // 保存出参
        if (request.getOutputParameters() != null && !request.getOutputParameters().isEmpty()) {
            int order = 1;
            for (SkillCreateRequest.OutputParameterDTO paramDTO : request.getOutputParameters()) {
                // 验证参数类型
                if (paramDTO.getParamType() != null && !paramDTO.getParamType().isEmpty()) {
                    ParameterTypeValidator.validateType(paramDTO.getParamType(), paramDTO.getParamName());
                }
                SkillParameterEntity param = new SkillParameterEntity();
                param.setSkillId(skillId);
                param.setParamDirection(SkillParamDirection.OUTPUT.name());
                param.setParamOrder(order++);
                param.setParamType(paramDTO.getParamType());
                param.setParamName(paramDTO.getParamName());
                param.setDescription(paramDTO.getDescription());
                param.setRequired(paramDTO.getRequired());
                parameterMapper.insert(param);
            }
        }

        // 保存访问控制
        if (request.getAccessControls() != null && !request.getAccessControls().isEmpty()) {
            for (SkillCreateRequest.AccessControlDTO acDTO : request.getAccessControls()) {
                SkillAccessControlEntity ac = new SkillAccessControlEntity();
                ac.setSkillId(skillId);
                ac.setTargetType(acDTO.getTargetType());
                ac.setTargetId(acDTO.getTargetId());
                accessControlMapper.insert(ac);
            }
        }

        log.info("Skill创建成功: ID={}", skillId);
        return getSkillById(skillId);
    }

    @Override
    @Transactional(readOnly = true)
    public SkillResponse getSkillById(String id) {
        SkillEntity skill = skillMapper.selectById(id);
        if (skill == null || skill.getDeleted()) {
            throw BusinessException.notFound("Skill", id);
        }
        return convertToResponseWithDetails(skill);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SkillResponse> getSkillList(Pageable pageable) {
        Page<SkillEntity> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillEntity::getDeleted, false)
               .orderByDesc(SkillEntity::getCreatedAt);
        IPage<SkillEntity> result = skillMapper.selectPage(page, wrapper);
        return convertToSpringPage(result);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SkillResponse> searchSkills(SkillQueryRequest query, Pageable pageable) {
        Page<SkillEntity> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());

        LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillEntity::getDeleted, false);

        if (query.getName() != null && !query.getName().isEmpty()) {
            wrapper.like(SkillEntity::getName, query.getName());
        }
        if (query.getExecutionType() != null && !query.getExecutionType().isEmpty()) {
            wrapper.eq(SkillEntity::getExecutionType, query.getExecutionType());
        }
        if (query.getCategory() != null && !query.getCategory().isEmpty()) {
            wrapper.eq(SkillEntity::getCategory, query.getCategory());
        }
        if (query.getAccessType() != null && !query.getAccessType().isEmpty()) {
            wrapper.eq(SkillEntity::getAccessType, query.getAccessType());
        }
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(SkillEntity::getStatus, query.getStatus());
        }
        if (query.getCreatedBy() != null && !query.getCreatedBy().isEmpty()) {
            wrapper.eq(SkillEntity::getCreatedBy, query.getCreatedBy());
        }
        if (query.getIsContainer() != null) {
            wrapper.eq(SkillEntity::getIsContainer, query.getIsContainer());
        }

        wrapper.orderByDesc(SkillEntity::getCreatedAt);

        IPage<SkillEntity> result = skillMapper.selectPage(page, wrapper);
        return convertToSpringPage(result);
    }

    @Override
    @Transactional
    public SkillResponse updateSkill(String id, SkillUpdateRequest request, MultipartFile file) {
        log.info("更新Skill: {}", id);

        SkillEntity skill = skillMapper.selectById(id);
        if (skill == null || skill.getDeleted()) {
            throw BusinessException.notFound("Skill", id);
        }

        // 检查名称是否与其他Skill重复
        if (request.getName() != null && !request.getName().equals(skill.getName())) {
            Long count = skillMapper.countByName(request.getName());
            if (count > 0) {
                throw BusinessException.conflict("Skill名称已存在: " + request.getName());
            }
            skill.setName(request.getName());
        }

        if (request.getDescription() != null) {
            skill.setDescription(request.getDescription());
        }
        // 如果上传了新文件，更新套件路径和文件名
        if (file != null && !file.isEmpty()) {
            String suitePath = saveSuiteFile(file);
            skill.setSuitePath(suitePath);
            skill.setSuiteFilename(file.getOriginalFilename());
        }
        if (request.getExecutionType() != null) {
            skill.setExecutionType(request.getExecutionType());
        }
        if (request.getCategory() != null) {
            skill.setCategory(request.getCategory());
        }
        if (request.getAccessType() != null) {
            skill.setAccessType(request.getAccessType());
        }
        if (request.getIsContainer() != null) {
            skill.setIsContainer(request.getIsContainer());
        }
        if (request.getAllowAddInputParams() != null) {
            skill.setAllowAddInputParams(request.getAllowAddInputParams());
        }
        if (request.getAllowAddOutputParams() != null) {
            skill.setAllowAddOutputParams(request.getAllowAddOutputParams());
        }
        if (request.getUpdatedBy() != null) {
            skill.setUpdatedBy(request.getUpdatedBy());
        }

        skillMapper.updateById(skill);

        // 更新入参（先删除旧的，再插入新的）
        if (request.getInputParameters() != null) {
            parameterMapper.deleteBySkillIdAndDirection(id, SkillParamDirection.INPUT);
            int order = 1;
            for (SkillUpdateRequest.InputParameterDTO paramDTO : request.getInputParameters()) {
                // 验证参数类型
                if (paramDTO.getParamType() != null && !paramDTO.getParamType().isEmpty()) {
                    ParameterTypeValidator.validateType(paramDTO.getParamType(), paramDTO.getParamName());
                }
                SkillParameterEntity param = new SkillParameterEntity();
                param.setSkillId(id);
                param.setParamDirection(SkillParamDirection.INPUT.name());
                param.setParamOrder(order++);
                param.setParamType(paramDTO.getParamType());
                param.setParamName(paramDTO.getParamName());
                param.setDefaultValue(paramDTO.getDefaultValue());
                param.setDescription(paramDTO.getDescription());
                param.setRequired(paramDTO.getRequired());
                parameterMapper.insert(param);
            }
        }

        // 更新出参（先删除旧的，再插入新的）
        if (request.getOutputParameters() != null) {
            parameterMapper.deleteBySkillIdAndDirection(id, SkillParamDirection.OUTPUT);
            int order = 1;
            for (SkillUpdateRequest.OutputParameterDTO paramDTO : request.getOutputParameters()) {
                // 验证参数类型
                if (paramDTO.getParamType() != null && !paramDTO.getParamType().isEmpty()) {
                    ParameterTypeValidator.validateType(paramDTO.getParamType(), paramDTO.getParamName());
                }
                SkillParameterEntity param = new SkillParameterEntity();
                param.setSkillId(id);
                param.setParamDirection(SkillParamDirection.OUTPUT.name());
                param.setParamOrder(order++);
                param.setParamType(paramDTO.getParamType());
                param.setParamName(paramDTO.getParamName());
                param.setDescription(paramDTO.getDescription());
                param.setRequired(paramDTO.getRequired());
                parameterMapper.insert(param);
            }
        }

        return getSkillById(id);
    }

    @Override
    @Transactional
    public void deleteSkill(String id) {
        log.info("删除Skill: {}", id);
        SkillEntity skill = skillMapper.selectById(id);
        if (skill == null || skill.getDeleted()) {
            throw BusinessException.notFound("Skill", id);
        }

        // 删除关联的参数
        parameterMapper.deleteBySkillId(id);

        // 删除关联的访问控制
        accessControlMapper.deleteBySkillId(id);

        // 删除执行套件文件
        deleteSuiteFile(skill.getSuitePath());

        // 软删除Skill记录（设置deleted=true和deletedAt）
        skill.setDeleted(true);
        skill.setDeletedAt(LocalDateTime.now());
        skillMapper.updateById(skill);
    }

    /**
     * 删除执行套件文件
     *
     * @param suitePath 套件文件路径
     */
    private void deleteSuiteFile(String suitePath) {
        if (suitePath == null || suitePath.isEmpty()) {
            return;
        }
        try {
            Path filePath = Paths.get(suitePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("执行套件文件删除成功: {}", suitePath);
            }
        } catch (IOException e) {
            log.warn("删除执行套件文件失败: {}", suitePath, e);
            // 文件删除失败不影响Skill删除
        }
    }

    @Override
    @Transactional
    public SkillResponse publishSkill(String id) {
        log.info("发布Skill: {}", id);

        SkillEntity skill = skillMapper.selectById(id);
        if (skill == null || skill.getDeleted()) {
            throw BusinessException.notFound("Skill", id);
        }

        skill.setStatus(SkillStatus.PUBLISHED.name());
        skillMapper.updateById(skill);

        return convertToResponse(skill);
    }

    @Override
    @Transactional
    public SkillResponse unpublishSkill(String id) {
        log.info("取消发布Skill: {}", id);

        SkillEntity skill = skillMapper.selectById(id);
        if (skill == null || skill.getDeleted()) {
            throw BusinessException.notFound("Skill", id);
        }

        skill.setStatus(SkillStatus.DRAFT.name());
        skillMapper.updateById(skill);

        return convertToResponse(skill);
    }

    @Override
    @Transactional
    public SkillResponse copySkill(String id) {
        log.info("复制Skill: {}", id);

        SkillEntity original = skillMapper.selectById(id);
        if (original == null || original.getDeleted()) {
            throw BusinessException.notFound("Skill", id);
        }

        // 创建新Skill
        SkillEntity newSkill = new SkillEntity();
        newSkill.setName(original.getName() + " (副本)");
        newSkill.setDescription(original.getDescription());
        newSkill.setSuitePath(original.getSuitePath());
        newSkill.setExecutionType(original.getExecutionType());
        newSkill.setCategory(original.getCategory());
        newSkill.setAccessType(original.getAccessType());
        newSkill.setIsContainer(original.getIsContainer());
        newSkill.setStatus(SkillStatus.DRAFT.name());
        newSkill.setCreatedBy(original.getCreatedBy());
        newSkill.setUpdatedBy(original.getCreatedBy());
        newSkill.setDeleted(false);

        skillMapper.insert(newSkill);
        String newSkillId = newSkill.getId();

        // 复制所有参数
        List<SkillParameterEntity> params = parameterMapper.selectBySkillId(id);
        for (SkillParameterEntity param : params) {
            SkillParameterEntity newParam = new SkillParameterEntity();
            newParam.setSkillId(newSkillId);
            newParam.setParamDirection(param.getParamDirection());
            newParam.setParamOrder(param.getParamOrder());
            newParam.setParamType(param.getParamType());
            newParam.setParamName(param.getParamName());
            newParam.setDefaultValue(param.getDefaultValue());
            newParam.setDescription(param.getDescription());
            newParam.setRequired(param.getRequired());
            parameterMapper.insert(newParam);
        }

        // 复制访问控制
        List<SkillAccessControlEntity> accessControls = accessControlMapper.selectBySkillId(id);
        for (SkillAccessControlEntity ac : accessControls) {
            SkillAccessControlEntity newAc = new SkillAccessControlEntity();
            newAc.setSkillId(newSkillId);
            newAc.setTargetType(ac.getTargetType());
            newAc.setTargetId(ac.getTargetId());
            accessControlMapper.insert(newAc);
        }

        return getSkillById(newSkillId);
    }

    private SkillResponse convertToResponse(SkillEntity skill) {
        SkillResponse response = new SkillResponse();
        response.setId(skill.getId());
        response.setName(skill.getName());
        response.setDescription(skill.getDescription());
        response.setSuitePath(skill.getSuitePath());
        response.setSuiteFilename(skill.getSuiteFilename());
        response.setExecutionType(SkillExecutionType.valueOf(skill.getExecutionType()));
        response.setCategory(SkillCategory.valueOf(skill.getCategory()));
        response.setAccessType(SkillAccessType.valueOf(skill.getAccessType()));
        response.setIsContainer(skill.getIsContainer());
        response.setAllowAddInputParams(skill.getAllowAddInputParams());
        response.setAllowAddOutputParams(skill.getAllowAddOutputParams());
        response.setStatus(SkillStatus.valueOf(skill.getStatus()));
        response.setCreatedBy(skill.getCreatedBy());
        response.setCreatedAt(skill.getCreatedAt());
        response.setUpdatedBy(skill.getUpdatedBy());
        response.setUpdatedAt(skill.getUpdatedAt());
        // 查询入参和出参数量
        response.setInputParamCount(parameterMapper.countBySkillIdAndDirection(skill.getId(), SkillParamDirection.INPUT));
        response.setOutputParamCount(parameterMapper.countBySkillIdAndDirection(skill.getId(), SkillParamDirection.OUTPUT));
        return response;
    }

    private SkillResponse convertToResponseWithDetails(SkillEntity skill) {
        SkillResponse response = convertToResponse(skill);

        // 获取入参
        List<SkillParameterEntity> inputParams = parameterMapper.selectBySkillIdAndDirection(
                skill.getId(), SkillParamDirection.INPUT);
        response.setInputParameters(inputParams.stream()
                .map(this::convertToInputParamDTO)
                .collect(Collectors.toList()));

        // 获取出参
        List<SkillParameterEntity> outputParams = parameterMapper.selectBySkillIdAndDirection(
                skill.getId(), SkillParamDirection.OUTPUT);
        response.setOutputParameters(outputParams.stream()
                .map(this::convertToOutputParamDTO)
                .collect(Collectors.toList()));

        // 获取访问控制
        List<SkillAccessControlEntity> accessControls = accessControlMapper.selectBySkillId(skill.getId());
        response.setAccessControls(accessControls.stream()
                .map(this::convertToAccessControlDTO)
                .collect(Collectors.toList()));

        return response;
    }

    private SkillResponse.InputParameterDTO convertToInputParamDTO(SkillParameterEntity param) {
        SkillResponse.InputParameterDTO dto = new SkillResponse.InputParameterDTO();
        dto.setId(param.getId());
        dto.setParamOrder(param.getParamOrder());
        dto.setParamType(param.getParamType());
        dto.setParamName(param.getParamName());
        dto.setDefaultValue(param.getDefaultValue());
        dto.setDescription(param.getDescription());
        dto.setRequired(param.getRequired());
        return dto;
    }

    private SkillResponse.OutputParameterDTO convertToOutputParamDTO(SkillParameterEntity param) {
        SkillResponse.OutputParameterDTO dto = new SkillResponse.OutputParameterDTO();
        dto.setId(param.getId());
        dto.setParamOrder(param.getParamOrder());
        dto.setParamType(param.getParamType());
        dto.setParamName(param.getParamName());
        dto.setDescription(param.getDescription());
        dto.setRequired(param.getRequired());
        return dto;
    }

    private SkillResponse.AccessControlDTO convertToAccessControlDTO(SkillAccessControlEntity ac) {
        SkillResponse.AccessControlDTO dto = new SkillResponse.AccessControlDTO();
        dto.setId(ac.getId());
        dto.setTargetType(ac.getTargetType());
        dto.setTargetId(ac.getTargetId());
        return dto;
    }

    private org.springframework.data.domain.Page<SkillResponse> convertToSpringPage(IPage<SkillEntity> mybatisPage) {
        List<SkillResponse> content = mybatisPage.getRecords().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(
                content,
                PageRequest.of(
                        (int) mybatisPage.getCurrent() - 1,
                        (int) mybatisPage.getSize()
                ),
                mybatisPage.getTotal()
        );
    }

    /**
     * 保存执行套件文件
     *
     * @param file 上传的文件
     * @return 保存后的文件路径
     */
    private String saveSuiteFile(MultipartFile file) {
        try {
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            log.info("执行套件文件保存成功: {}", filePath.toString());
            return filePath.toString();
        } catch (IOException e) {
            log.error("保存执行套件文件失败", e);
            throw BusinessException.invalidParam("执行套件文件保存失败: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Resource> downloadSuite(String id) {
        SkillEntity skill = skillMapper.selectById(id);
        if (skill == null || skill.getDeleted()) {
            throw BusinessException.notFound("Skill", id);
        }

        String suitePath = skill.getSuitePath();
        if (suitePath == null || suitePath.isEmpty()) {
            throw BusinessException.invalidParam("该Skill没有关联的执行套件文件");
        }

        Path filePath = Paths.get(suitePath);
        if (!Files.exists(filePath)) {
            throw BusinessException.notFound("执行套件文件", suitePath);
        }

        Resource resource = new FileSystemResource(filePath);
        String filename = skill.getSuiteFilename();
        if (filename == null || filename.isEmpty()) {
            filename = filePath.getFileName().toString();
        }

        // 对文件名进行URL编码以支持中文
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }
}
