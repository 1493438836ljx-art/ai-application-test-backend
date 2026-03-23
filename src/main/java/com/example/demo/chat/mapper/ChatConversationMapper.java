package com.example.demo.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.chat.entity.ChatConversationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * AI对话Mapper接口
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversationEntity> {

    /**
     * 根据UUID查询
     *
     * @param conversationUuid 对话UUID
     * @return 对话
     */
    Optional<ChatConversationEntity> selectByConversationUuid(@Param("conversationUuid") String conversationUuid);

    /**
     * 根据用户ID分页查询
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 对话分页列表
     */
    IPage<ChatConversationEntity> selectByUserId(Page<ChatConversationEntity> page, @Param("userId") String userId);

    /**
     * 根据用户ID和状态分页查询
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @param status 状态
     * @return 对话分页列表
     */
    IPage<ChatConversationEntity> selectByUserIdAndStatus(
            Page<ChatConversationEntity> page,
            @Param("userId") String userId,
            @Param("status") String status
    );

    /**
     * 根据状态分页查询
     *
     * @param page   分页参数
     * @param status 状态
     * @return 对话分页列表
     */
    IPage<ChatConversationEntity> selectByStatus(Page<ChatConversationEntity> page, @Param("status") String status);

    /**
     * 统计用户的对话数量
     *
     * @param userId 用户ID
     * @return 数量
     */
    Long countByUserId(@Param("userId") String userId);
}
