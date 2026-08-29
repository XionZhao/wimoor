-- Agent上下文配置表：存储业务域规则、实体关联关系、常见流程等
-- 支持动态管理，无需改代码即可调整Agent的行为
CREATE TABLE IF NOT EXISTS `t_sys_tool_agent_context` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `context_type` varchar(50) NOT NULL COMMENT '配置类型：business_domain/entity_relation/field_alias/common_flow/page_hint',
  `context_key` varchar(100) NOT NULL COMMENT '配置键名，如 amazon_domain/erp_domain/material→purchase',
  `content` json NOT NULL COMMENT '配置内容(JSON格式)',
  `sort_order` int DEFAULT 0 COMMENT '排序序号',
  `is_enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用：0禁用/1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `operator` bigint unsigned DEFAULT NULL COMMENT '操作人',
  `createtime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `opttime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_context_type` (`context_type`),
  KEY `idx_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent上下文配置表';

-- 初始化业务域配置
INSERT INTO `t_sys_tool_agent_context` (`context_type`, `context_key`, `content`, `sort_order`, `remark`) VALUES
('business_domain', 'amazon_domain', '{"name":"Amazon域","scope":"所有 amz_ 开头的工具、settlement/shipment/advert 等工具","uniqueness":"groupid + marketplaceid + sku 唯一","required_params":["groupid","marketplaceid"],"param_dependency":"必须先获取groupid（amz_query_amazon_group），再获取marketplaceid（amz_query_market_by_group），才能查询业务数据","key_facts":["groupid = Amazon店铺分组ID，是Amazon域的核心参数","marketplaceid = Amazon站点/国家ID，由groupid派生","一个groupid下有多个marketplaceid（如US/DE/JP等站点）","数据唯一性：groupid + marketplaceid + sku 三者组合才能唯一定位一条Amazon数据","同一个SKU在不同店铺分组/站点下是不同的数据"]}', 1, 'Amazon业务域规则'),

('business_domain', 'erp_domain', '{"name":"ERP域","scope":"所有 erp_ 开头的工具，以及 material_/inv_/warehouse_/purchase_/shipment_ 等工具","required_params":[],"auto_injected_params":["shopid"],"param_dependency":"shopid 由系统自动注入（即客户登录上下文），用户无需手动传参","uniqueness_rules":{"物料/商品":"shopid + sku 唯一。SKU在整个租户内唯一，贯穿ERP所有功能模块（库存、采购、发货、盘点、组装等）","所有表单":"shopid + number 唯一。采购单号、入库单号、发货单号、盘点单号等表单编号都在租户内唯一"},"key_facts":["shopid 是客户登录的租户上下文，由系统自动注入，用户无需关心","SKU 是ERP系统的核心标识，shopid + sku 唯一定位一个物料","SKU 贯穿ERP所有功能：库存(inv_list)、采购(purchase_list)、发货(shipment_plan_list)、盘点(stockTaking_list)、组装(assembly_list)等","所有表单使用 shopid + number（表单编号）唯一","查询物料只需传 search(SKU或名称)，shopid由系统自动注入","查询ERP库存/采购/发货等不需要groupid和marketplaceid","warehouseid 是ERP库存查询的仓库维度参数","supplierid 是供应商维度参数"]}', 2, 'ERP业务域规则'),

('business_domain', 'cross_domain', '{"name":"跨域关联","description":"Amazon商品与ERP物料通过SKU关联，Listing的msku对应ERP的SKU","typical_flow":"Amazon订单中的SKU → ERP物料查询(material_list) → ERP库存(inv_list) → 采购(purchase_list)"}', 3, '跨域关联规则');

-- 初始化实体关联关系
INSERT INTO `t_sys_tool_agent_context` (`context_type`, `context_key`, `content`, `sort_order`, `remark`) VALUES
('entity_relation', 'group→marketplace', '{"from":"店铺分组(AmazonGroup)","to":"站点/国家(Marketplace)","relation":"一个店铺分组下有多个站点","link_field":"groupid","query_tool":"amz_query_market_by_group","description":"几乎所有Amazon接口都需要先获取groupid"}', 1, NULL),
('entity_relation', 'marketplace→country', '{"from":"站点(Marketplace)","to":"国家(Country)","relation":"每个站点对应一个国家","link_field":"marketplaceid","description":"站点ID即国家站点ID，含country代码和currency币种"}', 2, NULL),
('entity_relation', 'product→listing', '{"from":"商品(Product)","to":"Listing","relation":"一个商品对应多个站点的Listing","link_field":"asin","description":"商品有pid和sku，Listing有asin和msku"}', 3, NULL),
('entity_relation', 'product→inventory', '{"from":"商品(Product)","to":"FBA库存(Inventory)","relation":"商品的FBA库存按SKU+站点维度存储","link_field":"sku","query_tool":"amz_query_fba_inventory"}', 4, NULL),
('entity_relation', 'product→profit', '{"from":"商品(Product)","to":"利润(Profit)","relation":"商品有独立的利润计算配置","link_field":"pid","query_tool":"amz_query_profit_detail","description":"利润查询使用pid，不是sku"}', 5, NULL),
('entity_relation', 'warehouse→erp_inventory', '{"from":"仓库(Warehouse)","to":"ERP库存(Inventory)","relation":"ERP库存按仓库+SKU维度存储","link_field":"warehouseid","query_tool":"inv_list"}', 6, NULL),
('entity_relation', 'supplier→purchase', '{"from":"供应商(Supplier)","to":"采购单(Purchase)","relation":"采购单关联供应商","link_field":"supplierid","query_tool":"purchase_list"}', 7, NULL),
('entity_relation', 'purchase→inbound', '{"from":"采购单(Purchase)","to":"入库单(Inbound)","relation":"采购单到货后生成入库单","link_field":"purchaseid","description":"采购单审核后可生成入库单"}', 8, NULL),
('entity_relation', 'material→all_erp', '{"from":"物料(Material)","to":"采购单/库存/订单","relation":"物料是ERP的核心实体，shopid+sku唯一，贯穿所有ERP功能","link_field":"sku/materialid","query_tool":"material_list"}', 9, NULL);

-- 初始化常见查询流程
INSERT INTO `t_sys_tool_agent_context` (`context_type`, `context_key`, `content`, `sort_order`, `remark`) VALUES
('common_flow', 'sales_query', '{"name":"查询某店铺某站点的销售数据","description":"需要先获取店铺分组ID和站点ID，再查询销售图表","steps":[{"step":1,"tool":"amz_query_amazon_group","purpose":"获取groupid列表"},{"step":2,"tool":"amz_query_market_by_group","input_from_step":1,"input_field":"groupid","purpose":"获取站点列表"},{"step":3,"tool":"amz_query_chart_sales","input_fields":["groupid","marketplaceid"],"purpose":"查询销售趋势数据"}]}', 1, NULL),
('common_flow', 'product_inventory_profit', '{"name":"查询某商品的库存和利润","description":"商品信息→FBA库存→利润详情","steps":[{"step":1,"tool":"amz_query_product_info_list","purpose":"搜索商品，获取pid和sku"},{"step":2,"tool":"amz_query_fba_inventory","input_field":"sku","purpose":"查询FBA库存"},{"step":3,"tool":"amz_query_profit_detail","input_field":"pid","purpose":"查询利润详情"}]}', 2, NULL),
('common_flow', 'erp_material_stock', '{"name":"查询ERP物料和库存","description":"直接用SKU查询，不需要groupid","steps":[{"step":1,"tool":"material_list","purpose":"用SKU或名称搜索物料"},{"step":2,"tool":"inv_list","purpose":"查询ERP库存"}]}', 3, NULL),
('common_flow', 'purchase_to_inbound', '{"name":"查询采购到入库全流程","description":"采购单→入库→库存变化","steps":[{"step":1,"tool":"purchase_list","purpose":"查询采购单列表"},{"step":2,"tool":"purchase_detail","input_field":"id","purpose":"查看采购单详情"},{"step":3,"tool":"inv_list","purpose":"查询库存变化"}]}', 4, NULL);
