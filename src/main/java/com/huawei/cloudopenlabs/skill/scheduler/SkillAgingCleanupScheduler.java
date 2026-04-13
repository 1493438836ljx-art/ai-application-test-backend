/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.skill.scheduler;

import com.huawei.cloudopenlabs.skill.entity.SkillEntity;
import com.huawei.cloudopenlabs.skill.mapper.SkillAccessControlMapper;
import com.huawei.cloudopenlabs.skill.mapper.SkillParameterMapper;
import com.huawei.cloudopenlabs.skill.mapper.SkillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Skill数据老化清理定时任务
 * 每天凌晨0点5分执行，物理删除已删除超过1年的Skill相关数据
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
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
        log.info("Starting skill data aging cleanup task...");

        try {
            // 1. 查询已删除超过1年的Skill
            List<SkillEntity> agedSkills = skillMapper.selectDeletedOlderThan(AGING_DAYS);
            log.info("Found skills deleted more than{}days ago: {}", AGING_DAYS, agedSkills.size());

            if (agedSkills.isEmpty()) {
                log.info("No skill data to clean up");
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
                    log.info("Physically deleted skill: ID={}, Name={}", skill.getId(), skill.getName());
                } catch (Exception e) {
                    log.error("Failed to physically delete skill: ID={}, Name={}", skill.getId(), skill.getName(), e);
                }
            }

            log.info("Skill aging cleanup completed, physically deleted {} skill records", deletedCount);

        } catch (Exception e) {
            log.error("Skill data aging cleanup task failed", e);
        }
    }
}
