package com.example.demo.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.chat.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * AI消息Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {

    /**
     * 根据UUID查询
     *
     * @param messageUuid 消息UUID
     * @return 消息
     */
    Optional<ChatMessageEntity> selectByMessageUuid(@Param("messageUuid") String messageUuid);

    /**
     * 根据对话ID查询消息列表
     *
     * @param conversationId 对话ID
     * @return 消息列表
     */
    List<ChatMessageEntity> selectByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 获取对话的最后一条消息
     *
     * @param conversationId 对话ID
     * @return 最后一条消息
     */
    Optional<ChatMessageEntity> selectLastMessage(@Param("conversationId") Long conversationId);

    /**
     * 统计对话的消息数量
     *
     * @param conversationId 对话ID
     * @return 数量
     */
    Long countByConversationId(@Param("conversationId") Long conversationId);
}
