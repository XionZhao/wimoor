-- --------------------------------------------------------
-- 主机:                           wimoor.rwlb.rds.aliyuncs.com
-- 服务器版本:                        8.0.36 - Source distribution
-- 服务器操作系统:                      Linux
-- HeidiSQL 版本:                  12.20.0.7320
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- 导出  表 db_finance.fin_invoice 结构
CREATE TABLE IF NOT EXISTS `fin_invoice` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '发票ID',
  `invoice_code` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发票代码（传统/电子发票）',
  `invoice_no` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发票号码（传统/电子发票）',
  `digital_invoice_no` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数电发票号码（20位，全电发票唯一标识）',
  `original_invoice_id` bigint unsigned DEFAULT NULL COMMENT '原发票ID（红冲/作废时关联原票）',
  `invoice_kind` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发票票种：VAT_SPECIAL（专票）/VAT_NORMAL（普票）/MOTOR（机动车）',
  `business_type` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '特定业务类型（如：农产品/建筑服务/不动产/货物）',
  `source` varchar(20) COLLATE utf8mb4_bin DEFAULT 'ERP_IMPORT' COMMENT '发票来源：TAX_SYSTEM（税局直连）/MANUAL（手工）/OCR_SCAN（扫描）',
  `groupid` bigint unsigned DEFAULT NULL COMMENT '账套ID',
  `supplier_id` bigint unsigned DEFAULT NULL COMMENT '供应商/销方ID（关联t_erp_customer.id）',
  `seller_name` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '销方名称',
  `seller_tax_no` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '销方纳税人识别号',
  `buyer_name` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '购买方名称',
  `buyer_tax_no` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '购买方纳税人识别号',
  `invoice_date` date DEFAULT NULL COMMENT '开票日期',
  `drawer` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '开票人',
  `amount_without_tax` decimal(18,2) DEFAULT NULL COMMENT '不含税金额（系统自动汇总明细）',
  `tax_amount` decimal(18,2) DEFAULT NULL COMMENT '税额（系统自动汇总明细）',
  `amount_with_tax` decimal(18,2) DEFAULT NULL COMMENT '价税合计（系统自动汇总明细）',
  `currency` varchar(10) COLLATE utf8mb4_bin DEFAULT 'CNY' COMMENT '币种',
  `exchange_rate` decimal(18,6) DEFAULT '1.000000' COMMENT '汇率（外币折算人民币）',
  `is_positive` tinyint(1) DEFAULT '1' COMMENT '是否正数发票：1正数/0红字（负数）',
  `status` varchar(20) COLLATE utf8mb4_bin DEFAULT 'NORMAL' COMMENT '状态：NORMAL（正常）/CANCELLED（作废）/RED_ALL（全额红冲）/RED_PART（部分红冲）/ABNORMAL（异常）',
  `risk_level` varchar(20) COLLATE utf8mb4_bin DEFAULT 'LOW' COMMENT '发票风险等级：HIGH/MIDDLE/LOW/NORMAL',
  `remark` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注（承接税局备注字段，含抵扣/不抵扣说明）',
  `posting_status` tinyint DEFAULT '0' COMMENT '入账状态：0未入账/1已入账',
  `voucher_id` bigint DEFAULT NULL COMMENT '入账凭证ID',
  `reconcile_status` tinyint DEFAULT '0' COMMENT '对账状态：0未对账/1已对账',
  `reconcile_time` datetime DEFAULT NULL COMMENT '对账时间',
  `reconcile_by` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '对账人',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_digital_invoice` (`digital_invoice_no`) USING BTREE COMMENT '数电票唯一索引',
  UNIQUE KEY `uk_invoice_code_no` (`invoice_code`,`invoice_no`) USING BTREE COMMENT '传统票联合唯一（代码+号码）',
  KEY `idx_original_id` (`original_invoice_id`) USING BTREE,
  KEY `idx_supplier_id` (`supplier_id`) USING BTREE,
  KEY `idx_invoice_date` (`invoice_date`) USING BTREE,
  KEY `idx_groupid` (`groupid`) USING BTREE,
  KEY `idx_buyer_tax_no` (`buyer_tax_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='发票台账主表（支持数电票/红冲/多票种）';

-- 数据导出被取消选择。

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
