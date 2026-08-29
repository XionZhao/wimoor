-- --------------------------------------------------------
-- ERP采购订单凭证同步追踪表
-- 以采购订单为维度（一个订单 = 一个凭证），追踪凭证转换状态
-- 每晚检测订单closepaydate变更，判断是否全部SKU已完成付款
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS `fin_erp_order_voucher` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `groupid` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '租户ID（账簿）',
  `order_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '采购订单ID（t_erp_purchase_form.id）',
  `order_number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '采购订单编号',
  `warehouse_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '仓库名称',
  `supplier_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '供应商名称',
  `total_amount` decimal(18,2) DEFAULT NULL COMMENT '订单总金额',
  `voucher_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'payment' COMMENT '凭证类型：payment-付款凭证，inventory_transit-在途库存凭证，inventory_inbound-入库库存凭证',
  `voucher_id` bigint unsigned DEFAULT NULL COMMENT '关联的凭证ID',
  `sync_status` tinyint unsigned DEFAULT '0' COMMENT '同步状态：0-待同步，1-已同步，2-已变更（需重新同步）',
  `data_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '数据指纹（MD5）',
  `sync_time` datetime DEFAULT NULL COMMENT '同步时间',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建人',
  `modify_by` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_groupid_order_type` (`groupid`,`order_id`,`voucher_type`) USING BTREE,
  KEY `idx_voucher_id` (`voucher_id`) USING BTREE,
  KEY `idx_sync_status` (`sync_status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='ERP采购订单凭证同步追踪表';