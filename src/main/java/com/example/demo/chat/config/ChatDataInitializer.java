package com.example.demo.chat.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.chat.entity.ChatQuickQuestionEntity;
import com.example.demo.chat.mapper.ChatQuickQuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * AI聊天数据初始化器
 *
 * @author AI Test Platform Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatDataInitializer implements CommandLineRunner {

    private final ChatQuickQuestionMapper quickQuestionMapper;

    @Override
    public void run(String... args) {
        log.info("初始化AI聊天数据...");
        initQuickQuestions();
    }

    private void initQuickQuestions() {
        // 快捷问题初始化
        createQuickQuestionIfNotExists("💡", "如何创建测评集？", "evaluation", 1);
        createQuickQuestionIfNotExists("🔧", "环境管理怎么配置？", "config", 2);
        createQuickQuestionIfNotExists("📊", "如何查看测试报告？", "report", 3);
        createQuickQuestionIfNotExists("🚀", "快速入门指南", "guide", 4);
        createQuickQuestionIfNotExists("❓", "支持哪些测试类型？", "test", 5);
        createQuickQuestionIfNotExists("⚙️", "如何配置评估指标？", "config", 6);

        log.info("快捷问题数据初始化完成");
    }

    private void createQuickQuestionIfNotExists(String icon, String text, String category, int sortOrder) {
        // 检查是否已存在相同文本的问题
        LambdaQueryWrapper<ChatQuickQuestionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatQuickQuestionEntity::getCategory, category)
               .eq(ChatQuickQuestionEntity::getText, text);
        boolean exists = quickQuestionMapper.selectCount(wrapper) > 0;

        if (!exists) {
            ChatQuickQuestionEntity question = new ChatQuickQuestionEntity();
            question.setIcon(icon);
            question.setText(text);
            question.setCategory(category);
            question.setSortOrder(sortOrder);
            question.setEnabled(true);
            quickQuestionMapper.insert(question);
            log.debug("创建快捷问题: {}", text);
        }
    }
}
