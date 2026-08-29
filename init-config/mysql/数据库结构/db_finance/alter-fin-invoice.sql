-- 添加缺失的字段到fin_invoice表
ALTER TABLE `fin_invoice`
ADD COLUMN `invoice_type` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发票类型：DIGITAL_VAT/DIGITAL_NORMAL/TRADITIONAL/OVERSEAS' AFTER `invoice_kind`,
ADD COLUMN `created_by` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建人' AFTER `updated_time`,
ADD COLUMN `updated_by` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '更新人' AFTER `created_by`;
