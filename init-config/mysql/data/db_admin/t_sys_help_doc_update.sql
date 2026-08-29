-- 添加path字段，存储对应的页面路径
ALTER TABLE `t_sys_help_doc` 
ADD COLUMN `path` VARCHAR(200) NULL DEFAULT NULL COMMENT '页面路径（用于根据URL匹配帮助文档）' AFTER `keywords`,
ADD INDEX `idx_path` (`path`);

-- 更新现有数据的path字段
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/list' WHERE `doc_key` = 'purchase';
UPDATE `t_sys_help_doc` SET `path` = '/erp/inventory/list' WHERE `doc_key` = 'inventory';
UPDATE `t_sys_help_doc` SET `path` = '/erp/inventory/stocktaking' WHERE `doc_key` = 'stocktake';
UPDATE `t_sys_help_doc` SET `path` = '/erp/ship/plan/list' WHERE `doc_key` = 'shipment';
UPDATE `t_sys_help_doc` SET `path` = '/amazon/fba/inventory' WHERE `doc_key` = 'fba_inventory';
UPDATE `t_sys_help_doc` SET `path` = '/amazon/sale/order/list' WHERE `doc_key` = 'order';
UPDATE `t_sys_help_doc` SET `path` = '/amazon/advertising/campaign' WHERE `doc_key` = 'advertising';
UPDATE `t_sys_help_doc` SET `path` = '/finance/voucher/list' WHERE `doc_key` = 'finance';
UPDATE `t_sys_help_doc` SET `path` = '/sys/user' WHERE `doc_key` = 'setting';

-- 如果需要，可以添加更多路径映射
-- UPDATE `t_sys_help_doc` SET `path` = '/erp/material/list' WHERE `doc_key` = 'material';
-- UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/base' WHERE `doc_key` = 'warehouse';
