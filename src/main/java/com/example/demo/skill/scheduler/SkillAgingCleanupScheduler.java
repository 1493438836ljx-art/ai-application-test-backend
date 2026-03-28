package com.example.demo.skill.scheduler;

import com.example.demo.skill.entity.SkillEntity;
import com.example.demo.skill.mapper.SkillAccessControlMapper;
import com.example.demo.skill.mapper.SkillParameterMapper;
import com.example.demo.skill.mapper.SkillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Skill数据老化清理定时任务
 * 每天凌晨0点5分执行，物理删除已删除超过1年的Skill相关数据
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillAgingCleanupScheduler {

    private final SkillMapper skillMapper;
    private final SkillParameterMapper parameterMapper;
    private final SkillAccessControlMapper accessControlMapper;

    /**
     * 老化清理阈值（天）
     */
    private static final int AGING_DAYS = 365;

    /**
     * 每天凌晨0点5分执行老化清理任务
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void cleanupAgedSkillData() {
        log.info("开始执行Skill数据老化清理任务...");

        try {
            // 1. 查询已删除超过1年的Skill
            List<SkillEntity> agedSkills = skillMapper.selectDeletedOlderThan(AGING_DAYS);
            log.info("发现已删除超过{}天的Skill数量: {}", AGING_DAYS, agedSkills.size());

            if (agedSkills.isEmpty()) {
                log.info("没有需要清理的Skill数据");
                return;
            }

            // 2. 逐个物理删除Skill及其关联数据
            int deletedCount = 0;
            for (SkillEntity skill : agedSkills) {
                try {
                    // 物理删除关联的参数
                    parameterMapper.deleteBySkillId(skill.getId());

                    // 物理删除关联的访问控制
                    accessControlMapper.deleteBySkillId(skill.getId());

                    // 物理删除Skill记录
                    skillMapper.physicalDeleteById(skill.getId());

                    deletedCount++;
                    log.info("已物理删除Skill: ID={}, Name={}", skill.getId(), skill.getName());
                } catch (Exception e) {
                    log.error("物理删除Skill失败: ID={}, Name={}", skill.getId(), skill.getName(), e);
                }
            }

            log.info("Skill数据老化清理任务完成，共物理删除 {} 条Skill记录", deletedCount);

        } catch (Exception e) {
            log.error("Skill数据老化清理任务失败", e);
        }
    }
}
