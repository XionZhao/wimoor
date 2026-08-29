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

-- 导出  表 db_finance.fin_mapping_erp_inventory 结构
CREATE TABLE IF NOT EXISTS `fin_mapping_erp_inventory` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `groupid` bigint unsigned NOT NULL COMMENT '租户ID（账簿）',
  `warehouse_type` tinyint(1) NOT NULL COMMENT '仓库类型：1-本地仓，2-FBA仓，3-海外仓',
  `stage` int DEFAULT NULL COMMENT '阶段：1-在途确认（付款时），2-入库验收（收货时）',
  `debit_subject_id` bigint unsigned NOT NULL COMMENT '借方科目ID（在途物资/库存商品）',
  `credit_subject_id` bigint unsigned NOT NULL COMMENT '贷方科目ID（应付暂估/预付在途）',
  `debit_auxiliary_type` varchar(30) COLLATE utf8mb4_bin DEFAULT 'SKU' COMMENT '借方辅助核算（通常为SKU）',
  `credit_auxiliary_type` varchar(30) COLLATE utf8mb4_bin DEFAULT 'SUPPLIER' COMMENT '贷方辅助核算（通常为供应商）',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `priority` int DEFAULT '100',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_groupid_type_stage` (`groupid`,`warehouse_type`,`stage`) USING BTREE,
  KEY `idx_groupid` (`groupid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='存货科目映射表（在途/入库凭证引擎）';

-- 数据导出被取消选择。

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
