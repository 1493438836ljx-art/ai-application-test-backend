/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.skill.entity;

/**
 * Skill access type enum
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
public enum SkillAccessType {
    /**
     * Public access
     */
    PUBLIC,

    /**
     * Private access (creator only)
     */
    PRIVATE,

    /**
     * Whitelist access
     */
    WHITELIST,

    /**
     * Project member access
     */
    PROJECT
}
