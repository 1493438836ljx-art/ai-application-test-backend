-- Agent Session Table - For multi-round conversation management
-- Stores session state with Claude Code, supports context persistence
CREATE TABLE IF NOT EXISTS agent_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key ID',
    conversation_id VARCHAR(36) NOT NULL COMMENT 'Conversation ID (maps to chat_conversation.conversation_uuid)',
    workflow_id BIGINT DEFAULT NULL COMMENT 'Associated workflow ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Session status (ACTIVE/COMPLETED/ERROR)',
    query_results TEXT DEFAULT NULL COMMENT 'Query results stored in JSON format',
    action_results TEXT DEFAULT NULL COMMENT 'Action results stored in JSON format',
    last_reasoning TEXT DEFAULT NULL COMMENT 'Last AI reasoning content',
    round_count INT NOT NULL DEFAULT 0 COMMENT 'Current round count',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_conversation_id (conversation_id),
    INDEX idx_workflow_id (workflow_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent session table';
