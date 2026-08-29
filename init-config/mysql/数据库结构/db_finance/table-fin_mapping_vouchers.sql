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

-- 导出  表 db_finance.fin_mapping_vouchers 结构
CREATE TABLE IF NOT EXISTS `fin_mapping_vouchers` (
  `id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `mapping_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '映射规则ID',
  `groupid` BIGINT(20) UNSIGNED NULL DEFAULT NULL COMMENT '账套ID',
  `vourches_id` BIGINT(20) UNSIGNED NULL DEFAULT NULL COMMENT '凭证ID',
  `datalog` TEXT NULL DEFAULT NULL,
  `voucher_date` DATETIME NULL DEFAULT NULL,
  `created_time` DATETIME NULL DEFAULT NULL,
  `updated_time` DATETIME NULL DEFAULT NULL,
  `modify_by` VARCHAR(10) NULL DEFAULT NULL,
  `create_by` VARCHAR(10) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `mapping_id_groupid_voucher_date` (`mapping_id`, `groupid`, `voucher_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin AUTO_INCREMENT=200 COMMENT='映射凭证关联表';

-- 数据导出被取消选择。

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;