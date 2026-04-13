/*
* Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
*/
package com.huawei.cloudopenlabs.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huawei.cloudopenlabs.chat.entity.ChatQuickQuestionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 快捷问题Mapper接口
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 * @since 2026-04-13
 */
@Mapper
public interface ChatQuickQuestionMapper extends BaseMapper<ChatQuickQuestionEntity> {

    /**
     * 查询所有启用的快捷问题
     *
     * @return 快捷问题列表
     */
    List<ChatQuickQuestionEntity> selectEnabled();

    /**
     * 根据分类查询启用的快捷问题
     *
     * @param category 分类
     * @return 快捷问题列表
     */
    List<ChatQuickQuestionEntity> selectEnabledByCategory(@Param("category") String category);
}
