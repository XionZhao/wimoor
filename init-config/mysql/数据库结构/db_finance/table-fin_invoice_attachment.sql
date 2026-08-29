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

-- 导出  表 db_finance.fin_invoice_attachment 结构
CREATE TABLE IF NOT EXISTS `fin_invoice_attachment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `invoice_id` bigint unsigned NOT NULL COMMENT '关联发票主表ID（fin_invoice.id）',
  `file_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '文件原始名称（如：发票影像.jpg）',
  `file_url` varchar(1000) COLLATE utf8mb4_bin NOT NULL COMMENT '文件存储路径（OSS/本地路径/Base64）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（单位：字节）',
  `file_hash` varchar(64) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '文件MD5/SHA-256哈希值（用于去重和防篡改）',
  `mime_type` varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '文件类型（如：image/jpeg、application/pdf）',
  `is_primary` tinyint(1) DEFAULT '0' COMMENT '是否主图/缩略图：1是/0否（用于预览展示）',
  `attachment_type` varchar(20) COLLATE utf8mb4_bin DEFAULT 'IMAGE' COMMENT '附件类型：IMAGE（影像件）/PDF（电子票）/XML（数电XML）/OTHER（其他）',
  `upload_by` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '上传人（操作员账号）',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `remark` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注（如：扫描件/电子原件）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_invoice_id` (`invoice_id`) USING BTREE,
  KEY `idx_file_hash` (`file_hash`) USING BTREE COMMENT '用于重复文件检测'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin ROW_FORMAT=DYNAMIC COMMENT='发票附件明细表（支持多附件、审计留痕）';

-- 数据导出被取消选择。

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
