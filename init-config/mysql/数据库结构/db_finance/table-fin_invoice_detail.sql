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

-- 导出  表 db_finance.fin_invoice_detail 结构
CREATE TABLE IF NOT EXISTS `fin_invoice_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '明细行ID',
  `invoice_id` bigint unsigned NOT NULL COMMENT '关联主表 fin_invoice.id',
  `line_no` int DEFAULT '1' COMMENT '税局原始行号（1,2,3...）',
  `goods_name` varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '货物或应税劳务名称',
  `spec_model` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '规格型号',
  `unit` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '单位',
  `quantity` decimal(18,4) DEFAULT NULL COMMENT '数量',
  `unit_price` decimal(18,6) DEFAULT NULL COMMENT '不含税单价',
  `amount_without_tax` decimal(18,2) DEFAULT NULL COMMENT '该行不含税金额',
  `tax_rate` decimal(10,4) DEFAULT NULL COMMENT '税率（如 13.0000）',
  `tax_amount` decimal(18,2) DEFAULT NULL COMMENT '该行税额',
  `amount_with_tax` decimal(18,2) DEFAULT NULL COMMENT '该行价税合计',
  `tax_category_code` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '该行税收分类编码（可能每行不同）',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_invoice_id` (`invoice_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='发票明细行表（支持一张发票多行商品）';

-- 数据导出被取消选择。

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
