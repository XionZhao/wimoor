-- 承运商对账记录表
CREATE TABLE IF NOT EXISTS `fin_carrier_reconcile_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `groupid` VARCHAR(64) NOT NULL COMMENT '租户ID',
  `carrier_id` VARCHAR(64) NOT NULL COMMENT '承运商ID',
  `carrier_name` VARCHAR(255) DEFAULT NULL COMMENT '承运商名称',
  `company_name` VARCHAR(255) DEFAULT NULL COMMENT '公司名称',
  `reconcile_month` VARCHAR(10) NOT NULL COMMENT '对账月份，格式：yyyy-MM',
  `total_plan_qty` INT DEFAULT 0 COMMENT '计划发货数量',
  `total_actual_qty` INT DEFAULT 0 COMMENT '实际发货数量',
  `total_received_qty` INT DEFAULT 0 COMMENT '实际接收数量',
  `total_ship_fee` DECIMAL(20,2) DEFAULT 0.00 COMMENT '运输费用',
  `total_other_fee` DECIMAL(20,2) DEFAULT 0.00 COMMENT '关税/其他费用',
  `total_worth` DECIMAL(20,2) DEFAULT 0.00 COMMENT '发货货值',
  `total_shipment_num` INT DEFAULT 0 COMMENT '货件票数',
  `reconcile_by` VARCHAR(64) DEFAULT NULL COMMENT '对账人',
  `reconcile_time` DATETIME DEFAULT NULL COMMENT '对账时间',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_groupid_carrier_month` (`groupid`, `carrier_id`, `reconcile_month`),
  KEY `idx_groupid_month` (`groupid`, `reconcile_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='承运商对账记录表';
