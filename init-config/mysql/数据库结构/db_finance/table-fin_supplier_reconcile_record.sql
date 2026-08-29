-- 供应商对账记录表
CREATE TABLE IF NOT EXISTS `fin_supplier_reconcile_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `groupid` VARCHAR(64) NOT NULL COMMENT '租户ID',
  `supplier_id` VARCHAR(64) NOT NULL COMMENT '供应商ID',
  `supplier_name` VARCHAR(255) DEFAULT NULL COMMENT '供应商名称',
  `company_name` VARCHAR(255) DEFAULT NULL COMMENT '公司名称',
  `reconcile_month` VARCHAR(10) NOT NULL COMMENT '对账月份，格式：yyyy-MM',
  `order_count` INT DEFAULT 0 COMMENT '订单数',
  `total_order_amount` DECIMAL(20,2) DEFAULT 0.00 COMMENT '订单总额（采购汇总）',
  `total_received` INT DEFAULT 0 COMMENT '已收货数量',
  `total_paid_amount` DECIMAL(20,2) DEFAULT 0.00 COMMENT '已付总额（付款汇总）',
  `total_unpaid_amount` DECIMAL(20,2) DEFAULT 0.00 COMMENT '未付总额',
  `total_invoiced_amount` DECIMAL(20,2) DEFAULT 0.00 COMMENT '已开票总额（发票汇总）',
  `total_uninvoiced_amount` DECIMAL(20,2) DEFAULT 0.00 COMMENT '未开票总额',
  `reconcile_by` VARCHAR(64) DEFAULT NULL COMMENT '对账人',
  `reconcile_time` DATETIME DEFAULT NULL COMMENT '对账时间',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_groupid_supplier_month` (`groupid`, `supplier_id`, `reconcile_month`),
  KEY `idx_groupid_month` (`groupid`, `reconcile_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商对账记录表';

-- 如果表已存在，需要先删除旧索引再添加唯一约束
-- ALTER TABLE fin_supplier_reconcile_record DROP INDEX idx_groupid_supplier_month;
-- ALTER TABLE fin_supplier_reconcile_record ADD UNIQUE KEY uk_groupid_supplier_month (groupid, supplier_id, reconcile_month);
