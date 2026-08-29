-- 发票台账相关表初始化脚本

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `db_finance` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

USE `db_finance`;

-- 发票台账主表
CREATE TABLE IF NOT EXISTS `fin_invoice` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '发票ID',
  `invoice_code` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发票代码',
  `invoice_no` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发票号码',
  `digital_invoice_no` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数电发票号码',
  `original_invoice_id` bigint unsigned DEFAULT NULL COMMENT '原发票ID',
  `invoice_kind` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发票票种',
  `business_type` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '特定业务类型',
  `source` varchar(20) COLLATE utf8mb4_bin DEFAULT 'ERP_IMPORT' COMMENT '发票来源',
  `groupid` bigint unsigned DEFAULT NULL COMMENT '账套ID',
  `supplier_id` bigint unsigned DEFAULT NULL COMMENT '供应商ID',
  `seller_name` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '销方名称',
  `seller_tax_no` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '销方纳税人识别号',
  `buyer_name` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '购买方名称',
  `buyer_tax_no` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '购买方纳税人识别号',
  `invoice_date` date DEFAULT NULL COMMENT '开票日期',
  `drawer` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '开票人',
  `amount_without_tax` decimal(18,2) DEFAULT NULL COMMENT '不含税金额',
  `tax_amount` decimal(18,2) DEFAULT NULL COMMENT '税额',
  `amount_with_tax` decimal(18,2) DEFAULT NULL COMMENT '价税合计',
  `currency` varchar(10) COLLATE utf8mb4_bin DEFAULT 'CNY' COMMENT '币种',
  `exchange_rate` decimal(18,6) DEFAULT '1.000000' COMMENT '汇率',
  `is_positive` tinyint(1) DEFAULT '1' COMMENT '是否正数发票：1正数/0红字',
  `status` varchar(20) COLLATE utf8mb4_bin DEFAULT 'NORMAL' COMMENT '状态',
  `risk_level` varchar(20) COLLATE utf8mb4_bin DEFAULT 'LOW' COMMENT '风险等级',
  `remark` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注',
  `posting_status` tinyint DEFAULT '0' COMMENT '入账状态：0未入账/1已入账',
  `voucher_id` bigint DEFAULT NULL COMMENT '入账凭证ID',
  `reconcile_status` tinyint DEFAULT '0' COMMENT '对账状态',
  `reconcile_time` datetime DEFAULT NULL COMMENT '对账时间',
  `reconcile_by` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '对账人',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_digital_invoice` (`digital_invoice_no`) USING BTREE,
  UNIQUE KEY `uk_invoice_code_no` (`invoice_code`,`invoice_no`) USING BTREE,
  KEY `idx_original_id` (`original_invoice_id`) USING BTREE,
  KEY `idx_supplier_id` (`supplier_id`) USING BTREE,
  KEY `idx_invoice_date` (`invoice_date`) USING BTREE,
  KEY `idx_groupid` (`groupid`) USING BTREE,
  KEY `idx_buyer_tax_no` (`buyer_tax_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='发票台账主表';

-- 发票明细行表
CREATE TABLE IF NOT EXISTS `fin_invoice_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '明细行ID',
  `invoice_id` bigint unsigned NOT NULL COMMENT '关联主表ID',
  `line_no` int DEFAULT '1' COMMENT '行号',
  `goods_name` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '货物或应税劳务名称',
  `spec_model` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '单位',
  `quantity` decimal(18,4) DEFAULT NULL COMMENT '数量',
  `unit_price` decimal(18,6) DEFAULT NULL COMMENT '不含税单价',
  `amount_without_tax` decimal(18,2) DEFAULT NULL COMMENT '不含税金额',
  `tax_rate` decimal(10,4) DEFAULT NULL COMMENT '税率',
  `tax_amount` decimal(18,2) DEFAULT NULL COMMENT '税额',
  `amount_with_tax` decimal(18,2) DEFAULT NULL COMMENT '价税合计',
  `tax_category_code` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '税收分类编码',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_invoice_id` (`invoice_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='发票明细行表';

-- 发票扩展信息表
CREATE TABLE IF NOT EXISTS `fin_invoice_extension` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint unsigned NOT NULL COMMENT '关联发票ID',
  `business_type` varchar(50) COLLATE utf8mb4_bin NOT NULL COMMENT '特定业务类型',
  `attr_key` varchar(100) COLLATE utf8mb4_bin NOT NULL COMMENT '属性名称',
  `attr_value` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '属性值',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_invoice_id` (`invoice_id`),
  KEY `idx_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='发票扩展信息表';

-- 发票附件表
CREATE TABLE IF NOT EXISTS `fin_invoice_attachment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint unsigned NOT NULL COMMENT '关联发票ID',
  `file_name` varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '文件名',
  `file_path` varchar(500) COLLATE utf8mb4_bin NOT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `file_type` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '文件类型',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_invoice_id` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='发票附件表';

-- 发票与采购订单关联表
CREATE TABLE IF NOT EXISTS `fin_invoice_po_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint unsigned NOT NULL COMMENT '发票ID',
  `po_id` bigint unsigned NOT NULL COMMENT '采购订单ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_po` (`invoice_id`,`po_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='发票采购订单关联表';

-- 发票与付款单关联表
CREATE TABLE IF NOT EXISTS `fin_invoice_payment_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint unsigned NOT NULL COMMENT '发票ID',
  `payment_id` bigint unsigned NOT NULL COMMENT '付款单ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_payment` (`invoice_id`,`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='发票付款单关联表';
