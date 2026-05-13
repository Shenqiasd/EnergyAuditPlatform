-- Migration 38: Rename unit_type=2 dictionary label from "分配输送" to "输送分配".
-- See Linear GRA-69 (EA-CUST-046).
--
-- Customer requested the second tab/dictionary label for bs_unit.unit_type=2
-- to read "输送分配" instead of "分配输送". The unit_type dict value (numeric
-- "2") and bs_unit.unit_type column values are unchanged; only the human-
-- readable label moves.
--
-- Idempotent: the WHERE clause matches the old label, so running this on a
-- database that has already been migrated is a no-op.
--
-- Also normalises any de_energy_flow.flow_stage rows that may have been left
-- with the historical "分配输送" Chinese label before EnergyFlowPostProcessor
-- translated them to "distribution".

UPDATE `sys_dict_data`
SET `dict_label` = '输送分配'
WHERE `dict_type` = 'unit_type'
  AND `dict_value` = '2'
  AND `dict_label` = '分配输送';

UPDATE `de_energy_flow`
SET `flow_stage` = 'distribution'
WHERE `deleted` = 0
  AND `flow_stage` IN ('分配输送', '输送分配');
