package com.example.demo.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.chat.entity.ChatFeedbackEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * AI反馈Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface ChatFeedbackMapper extends BaseMapper<ChatFeedbackEntity> {

    /**
     * 根据消息ID查询反馈
     *
     * @param messageId 消息ID
     * @return 反馈
     */
    Optional<ChatFeedbackEntity> selectByMessageId(@Param("messageId") Long messageId);

    /**
     * 根据用户ID查询反馈列表
     *
     * @param userId 用户ID
     * @return 反馈列表
     */
    java.util.List<ChatFeedbackEntity> selectByUserId(@Param("userId") String userId);
}
