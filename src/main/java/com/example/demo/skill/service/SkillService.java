package com.example.demo.skill.service;

import com.example.demo.skill.dto.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Skill服务接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
public interface SkillService {

    /**
     * 创建Skill
     *
     * @param request 创建请求
     * @param file    执行套件文件
     * @return Skill响应
     */
    SkillResponse createSkill(SkillCreateRequest request, MultipartFile file);

    /**
     * 获取Skill详情
     *
     * @param id Skill ID
     * @return Skill响应
     */
    SkillResponse getSkillById(String id);

    /**
     * 获取Skill列表
     *
     * @param pageable 分页参数
     * @return Skill分页列表
     */
    Page<SkillResponse> getSkillList(Pageable pageable);

    /**
     * 根据条件查询Skill列表
     *
     * @param query    查询条件
     * @param pageable 分页参数
     * @return Skill分页列表
     */
    Page<SkillResponse> searchSkills(SkillQueryRequest query, Pageable pageable);

    /**
     * 更新Skill
     *
     * @param id      Skill ID
     * @param request 更新请求
     * @param file    执行套件文件（可选）
     * @return Skill响应
     */
    SkillResponse updateSkill(String id, SkillUpdateRequest request, MultipartFile file);

    /**
     * 删除Skill
     *
     * @param id Skill ID
     */
    void deleteSkill(String id);

    /**
     * 发布Skill
     *
     * @param id Skill ID
     * @return Skill响应
     */
    SkillResponse publishSkill(String id);

    /**
     * 取消发布Skill
     *
     * @param id Skill ID
     * @return Skill响应
     */
    SkillResponse unpublishSkill(String id);

    /**
     * 复制Skill
     *
     * @param id Skill ID
     * @return 新Skill响应
     */
    SkillResponse copySkill(String id);

    /**
     * 下载执行套件
     *
     * @param id Skill ID
     * @return 执行套件文件资源
     */
    ResponseEntity<Resource> downloadSuite(String id);
}
