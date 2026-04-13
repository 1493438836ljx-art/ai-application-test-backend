/*
* Copyright(c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.agent.cache;

/**
 * Skill 缓存异常
 *
 * @author GNEEC LIVE
 * @version 27.0.3.0
 * @since 2026-04-13
 */
public class SkillCacheException extends RuntimeException {

    public SkillCacheException(String message) {
        super(message);
    }

    public SkillCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
