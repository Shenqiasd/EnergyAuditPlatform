-- Add equipment_name column to de_equipment_benchmark
-- This column stores the name/identifier of each equipment item across all device types
ALTER TABLE de_equipment_benchmark ADD COLUMN equipment_name VARCHAR(256) DEFAULT NULL AFTER equipment_type;
