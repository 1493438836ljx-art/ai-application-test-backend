-- Migration: V1.4__update_skill_library_schema.sql
-- Description: Update skill library schema - add allow_add_input_params and allow_add_output_params fields,
--              remove variadic field from parameter tables

-- Add new columns to skill table
ALTER TABLE skill ADD COLUMN allow_add_input_params TINYINT(1) DEFAULT 0 COMMENT '是否支持增加入参';
ALTER TABLE skill ADD COLUMN allow_add_output_params TINYINT(1) DEFAULT 0 COMMENT '是否支持增加出参';

-- Remove variadic column from skill_input_parameter table
ALTER TABLE skill_input_parameter DROP COLUMN variadic;

-- Remove variadic column from skill_output_parameter table
ALTER TABLE skill_output_parameter DROP COLUMN variadic;
