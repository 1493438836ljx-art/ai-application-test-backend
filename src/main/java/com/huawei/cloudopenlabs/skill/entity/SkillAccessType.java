package com.huawei.cloudopenlabs.skill.entity;

/**
 * Skill access type enum
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
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
