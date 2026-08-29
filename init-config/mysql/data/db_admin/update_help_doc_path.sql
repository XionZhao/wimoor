-- 更新帮助文档表的path字段，从菜单表中获取页面路径

-- 更新采购单
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/orders' WHERE `doc_key` = 'purchase';

-- 更新库存管理
UPDATE `t_sys_help_doc` SET `path` = '/erp/inventory/daily' WHERE `doc_key` = 'inventory';

-- 更新库存盘点
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/stocktake' WHERE `doc_key` = 'stocktake';

-- 更新发货准备
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/consumable' WHERE `doc_key` = 'shipment';

-- 更新供应商
UPDATE `t_sys_help_doc` SET `path` = '/erp/baseinfo/supplier' WHERE `doc_key` = 'supplier';

-- 更新产品管理
UPDATE `t_sys_help_doc` SET `path` = '/erp/product/localproduct' WHERE `doc_key` = 'material';

-- 更新订单
UPDATE `t_sys_help_doc` SET `path` = '/amazon/sale/order/list' WHERE `doc_key` = 'order';

-- 更新FBA费用
UPDATE `t_sys_help_doc` SET `path` = '/amazon/report/fbafee' WHERE `doc_key` = 'fba_fee';

-- 更新仓库列表
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/base' WHERE `doc_key` = 'warehouse';

-- 更新财务管理
UPDATE `t_sys_help_doc` SET `path` = '/finance/paymentRequest' WHERE `doc_key` = 'finance';

-- 更新系统设置
UPDATE `t_sys_help_doc` SET `path` = '/sys/subuser' WHERE `doc_key` = 'setting';

-- 更新利润计算
UPDATE `t_sys_help_doc` SET `path` = '/amazon/profit/calculator' WHERE `doc_key` = 'profit';

-- 更新计算方案
UPDATE `t_sys_help_doc` SET `path` = '/amazon/profit/config' WHERE `doc_key` = 'calc_scheme';

-- 更新SKU配对
UPDATE `t_sys_help_doc` SET `path` = '/amazon/listing/relation' WHERE `doc_key` = 'sku_match';

-- 更新标签管理
UPDATE `t_sys_help_doc` SET `path` = '/sys/tags' WHERE `doc_key` = 'tag';

-- 更新品牌管理
UPDATE `t_sys_help_doc` SET `path` = '/erp/baseinfo/brand' WHERE `doc_key` = 'brand';

-- 更新品类管理
UPDATE `t_sys_help_doc` SET `path` = '/erp/baseinfo/category' WHERE `doc_key` = 'category';

-- 更新辅料管理
UPDATE `t_sys_help_doc` SET `path` = '/erp/material/consumable' WHERE `doc_key` = 'accessory';

-- 更新箱子管理
UPDATE `t_sys_help_doc` SET `path` = '/erp/material/package' WHERE `doc_key` = 'box';

-- 更新采购规划
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/plan' WHERE `doc_key` = 'purchase_plan';

-- 更新加工单
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/process' WHERE `doc_key` = 'process_order';

-- 更新请款单
UPDATE `t_sys_help_doc` SET `path` = '/finance/paymentRequest' WHERE `doc_key` = 'payment_request';

-- 更新采购换货单
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/change' WHERE `doc_key` = 'exchange_order';

-- 更新采购入库明细
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/storageDetail' WHERE `doc_key` = 'purchase_inbound_detail';

-- 更新采购统计
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/summary' WHERE `doc_key` = 'purchase_statistics';

-- 更新采购付款明细
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/paymentDetail' WHERE `doc_key` = 'purchase_payment_detail';

-- 更新采购详情
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/detailinfo' WHERE `doc_key` = 'purchase_detail';

-- 更新采购时效
UPDATE `t_sys_help_doc` SET `path` = '/erp/purchase/time' WHERE `doc_key` = 'purchase_timeliness';

-- 更新商品分析
UPDATE `t_sys_help_doc` SET `path` = '/amazon/sale/listing' WHERE `doc_key` = 'product_analysis';

-- 更新调价队列
UPDATE `t_sys_help_doc` SET `path` = '/amazon/sale/priceadjust' WHERE `doc_key` = 'price_queue';

-- 更新趋势分析
UPDATE `t_sys_help_doc` SET `path` = '/amazon/listing/analysis' WHERE `doc_key` = 'trend_analysis';

-- 更新销售报表
UPDATE `t_sys_help_doc` SET `path` = '/amazon/sales/report' WHERE `doc_key` = 'sales_report';

-- 更新销量详情
UPDATE `t_sys_help_doc` SET `path` = '/amazon/sales/details' WHERE `doc_key` = 'sales_detail';

-- 更新销售计划
UPDATE `t_sys_help_doc` SET `path` = '/amazon/sales/forecast' WHERE `doc_key` = 'sales_plan';

-- 更新商品搜索
UPDATE `t_sys_help_doc` SET `path` = '/amazon/listing/catalog' WHERE `doc_key` = 'product_search';

-- 更新促销报告
UPDATE `t_sys_help_doc` SET `path` = '/amazon/report/performance' WHERE `doc_key` = 'promotion_report';

-- 更新透明计划
UPDATE `t_sys_help_doc` SET `path` = '/amazon/transparency/manager' WHERE `doc_key` = 'transparency';

-- 更新今日订单
UPDATE `t_sys_help_doc` SET `path` = '/amazon/sale/order/today' WHERE `doc_key` = 'today_order';

-- 更新流量报表
UPDATE `t_sys_help_doc` SET `path` = '/amazon/report/pageview' WHERE `doc_key` = 'traffic_report';

-- 更新FBA滞销
UPDATE `t_sys_help_doc` SET `path` = '/amazon/inventory/planning' WHERE `doc_key` = 'fba_slow';

-- 更新差评管理
UPDATE `t_sys_help_doc` SET `path` = '/report/feedback' WHERE `doc_key` = 'complaint';

-- 更新仓库地址
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/address' WHERE `doc_key` = 'warehouse_address';

-- 更新库位列表
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/shelf/create' WHERE `doc_key` = 'location';

-- 更新库位库存
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/shelf/inv' WHERE `doc_key` = 'location_inventory';

-- 更新海外仓
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/oversea' WHERE `doc_key` = 'overseas_warehouse';

-- 更新海外仓备货单
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/overseas/stock' WHERE `doc_key` = 'overseas_stock_order';

-- 更新多渠道订单
UPDATE `t_sys_help_doc` SET `path` = '/erp/order' WHERE `doc_key` = 'multi_channel_order';

-- 更新海外仓补发货规划
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/overseas/plan' WHERE `doc_key` = 'overseas_replenish';

-- 更新费用分摊
UPDATE `t_sys_help_doc` SET `path` = '/finance/costSharing' WHERE `doc_key` = 'fee_share';

-- 更新调库单
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/transfer' WHERE `doc_key` = 'transfer_order';

-- 更新代料单
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/exchange' WHERE `doc_key` = 'proxy_order';

-- 更新出库单
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/outbound' WHERE `doc_key` = 'outbound_order';

-- 更新入库单
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/inbound' WHERE `doc_key` = 'inbound_order';

-- 更新库存盘点（报表）
UPDATE `t_sys_help_doc` SET `path` = '/erp/warehouse/stocktake' WHERE `doc_key` = 'stocktake_report';

-- 更新库存报表
UPDATE `t_sys_help_doc` SET `path` = '/erp/inventory/report/base' WHERE `doc_key` = 'inventory_report';

-- 更新每日库存
UPDATE `t_sys_help_doc` SET `path` = '/erp/inventory/daily' WHERE `doc_key` = 'daily_inventory';

-- 更新出入库明细
UPDATE `t_sys_help_doc` SET `path` = '/erp/inventory/record' WHERE `doc_key` = 'in_out_detail';

-- 更新上下架明细
UPDATE `t_sys_help_doc` SET `path` = '/erp/inventory/shelf/operator' WHERE `doc_key` = 'shelf_detail';

-- 更新周转报告
UPDATE `t_sys_help_doc` SET `path` = '/erp/inventory/report/turns' WHERE `doc_key` = 'turnover_report';

-- 更新物流公司
UPDATE `t_sys_help_doc` SET `path` = '/erp/transportation' WHERE `doc_key` = 'logistics_company';

-- 更新发货地址
UPDATE `t_sys_help_doc` SET `path` = '/amazon/address' WHERE `doc_key` = 'ship_address';

-- 更新标签打印
UPDATE `t_sys_help_doc` SET `path` = '/amazon/product/label' WHERE `doc_key` = 'label_print';

-- 更新渠道管理
UPDATE `t_sys_help_doc` SET `path` = '/erp/thirdparty' WHERE `doc_key` = 'channel_manage';

-- 更新FBA发货规划
UPDATE `t_sys_help_doc` SET `path` = '/erp/ship/ship_plan' WHERE `doc_key` = 'fba_ship_plan';

-- 更新发货单
UPDATE `t_sys_help_doc` SET `path` = '/erp/shipv2/shipment_add/list' WHERE `doc_key` = 'ship_order';

-- 更新货件处理
UPDATE `t_sys_help_doc` SET `path` = '/erp/shipv2/shipment_handing' WHERE `doc_key` = 'shipment_process';

-- 更新货件跟踪
UPDATE `t_sys_help_doc` SET `path` = '/erp/ship/shipment_handing' WHERE `doc_key` = 'shipment_track';

-- 更新FBA库存
UPDATE `t_sys_help_doc` SET `path` = '/amazon/inventory/fba' WHERE `doc_key` = 'fba_inventory';

-- 更新FBA每日库存
UPDATE `t_sys_help_doc` SET `path` = '/amazon/inventory/fba_today' WHERE `doc_key` = 'fba_daily_inventory';

-- 更新广告管理
UPDATE `t_sys_help_doc` SET `path` = '/amazon/manager' WHERE `doc_key` = 'advertising';

-- 更新广告操作记录
UPDATE `t_sys_help_doc` SET `path` = '/amazon/a/record' WHERE `doc_key` = 'ad_operation_log';

-- 更新广告统计
UPDATE `t_sys_help_doc` SET `path` = '/amazon/a/summary' WHERE `doc_key` = 'ad_statistics';

-- 更新广告报表下载
UPDATE `t_sys_help_doc` SET `path` = '/amazon/a/download' WHERE `doc_key` = 'ad_report_download';

-- 更新结算记录
UPDATE `t_sys_help_doc` SET `path` = '/amazon/payment/record' WHERE `doc_key` = 'settlement_record';

-- 更新利润报表
UPDATE `t_sys_help_doc` SET `path` = '/finance/profitReport' WHERE `doc_key` = 'profit_report';

-- 更新账期SKU
UPDATE `t_sys_help_doc` SET `path` = '/finance/settlementsku' WHERE `doc_key` = 'account_sku';

-- 更新业绩报告
UPDATE `t_sys_help_doc` SET `path` = '/finance/performanpce' WHERE `doc_key` = 'performance_report';

-- 更新赔偿费用
UPDATE `t_sys_help_doc` SET `path` = '/amazon/report/reimbursements' WHERE `doc_key` = 'compensation_fee';

-- 更新FBA仓储费
UPDATE `t_sys_help_doc` SET `path` = '/amazon/report/storage' WHERE `doc_key` = 'fba_storage_fee';

-- 更新账期未结算
UPDATE `t_sys_help_doc` SET `path` = '/amazon/report/open' WHERE `doc_key` = 'unsettled';

-- 更新其它费用
UPDATE `t_sys_help_doc` SET `path` = '/finance/costSharing' WHERE `doc_key` = 'other_fee';

-- 更新批次成本
UPDATE `t_sys_help_doc` SET `path` = '/finance/jobCosting' WHERE `doc_key` = 'batch_cost';

-- 更新库存货值
UPDATE `t_sys_help_doc` SET `path` = '/finance/inventoryValue' WHERE `doc_key` = 'inventory_value';

-- 更新采购记账
UPDATE `t_sys_help_doc` SET `path` = '/finance/account' WHERE `doc_key` = 'purchase_accounting';

-- 更新1688绑定
UPDATE `t_sys_help_doc` SET `path` = '/erp/open1688/bind' WHERE `doc_key` = 'bind_1688';

-- 更新店铺管理
UPDATE `t_sys_help_doc` SET `path` = '/amazon/storeAuth' WHERE `doc_key` = 'shop_manage';

-- 更新物流绑定
UPDATE `t_sys_help_doc` SET `path` = '/erp/thirdparty' WHERE `doc_key` = 'logistics_bind';

-- 更新飞书绑定
UPDATE `t_sys_help_doc` SET `path` = '/sys/feishu' WHERE `doc_key` = 'feishu_bind';

-- 更新账号管理
UPDATE `t_sys_help_doc` SET `path` = '/sys/subuser' WHERE `doc_key` = 'account_manage';

-- 更新角色权限
UPDATE `t_sys_help_doc` SET `path` = '/sys/ruleAuth' WHERE `doc_key` = 'role_permission';

-- 更新凭证录入
UPDATE `t_sys_help_doc` SET `path` = '/finance/voucher/entry' WHERE `doc_key` = 'voucher_entry';

-- 更新查凭证
UPDATE `t_sys_help_doc` SET `path` = '/finance/voucher/query' WHERE `doc_key` = 'voucher_query';

-- 更新总账目
UPDATE `t_sys_help_doc` SET `path` = '/finance/ledger' WHERE `doc_key` = 'ledger';

-- 验证更新结果
SELECT `doc_key`, `title`, `path` FROM `t_sys_help_doc` WHERE `path` IS NOT NULL ORDER BY `sort_order`;
