CREATE TABLE `t_erp_customer_account` (
  `id` BIGINT(20) UNSIGNED NOT NULL COMMENT 'ID',
  `customer_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '供应商ID',
  `company_name` CHAR(50) NULL DEFAULT NULL COMMENT '公司名称' COLLATE 'utf8mb4_bin',
  `account_number` VARCHAR(50) NULL DEFAULT NULL COMMENT '银行账号' COLLATE 'utf8mb4_bin',
  `bank_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '开户行' COLLATE 'utf8mb4_bin',
  `shopid` BIGINT(20) UNSIGNED NULL DEFAULT NULL COMMENT '所属店铺（公司）',
  `operator` BIGINT(20) UNSIGNED NULL DEFAULT NULL COMMENT '操作人',
  `opttime` DATETIME NULL DEFAULT NULL COMMENT '修改时间',
  `is_default` TINYINT(1) NULL DEFAULT 0 COMMENT '是否默认账户（0-否 1-是）',
  `status` INT NULL DEFAULT 1 COMMENT '状态（1-启用 0-停用）',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注' COLLATE 'utf8mb4_bin',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `number_shopid` (`account_number`, `shopid`) USING BTREE,
  UNIQUE INDEX `name_shopid` (`shopid`, `company_name`) USING BTREE,
  INDEX `idx_customer_id` (`customer_id`) USING BTREE
)
COMMENT='供应商收款账户'
COLLATE='utf8mb4_bin'
ENGINE=InnoDB
ROW_FORMAT=DYNAMIC;
