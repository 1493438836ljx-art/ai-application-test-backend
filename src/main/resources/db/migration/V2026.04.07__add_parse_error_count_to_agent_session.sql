-- Add parse_error_count column to agent_session table
-- This field tracks the number of consecutive parse errors to prevent infinite loops

ALTER TABLE agent_session
ADD COLUMN parse_error_count INT NOT NULL DEFAULT 0 COMMENT 'Parse error count for limiting retry attempts'
AFTER round_count;
