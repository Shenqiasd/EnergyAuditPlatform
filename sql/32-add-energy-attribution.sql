-- GRA-39: 能源品种新增归属字段
CALL ensure_column('bs_energy', 'attribution',
    'VARCHAR(20) DEFAULT NULL COMMENT ''归属类型: 化石燃料/非化石燃料'' AFTER category');

CALL ensure_column('bs_energy_catalog', 'attribution',
    'VARCHAR(20) DEFAULT NULL COMMENT ''归属类型: 化石燃料/非化石燃料'' AFTER category');
