-- Agent自我学习记录表：存储Agent从交互中学到的知识
-- 与 t_sys_tool_agent_context（静态配置）独立，互不干扰
CREATE TABLE IF NOT EXISTS `t_sys_agent_learning` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `learn_type` varchar(50) NOT NULL COMMENT '学习类型：tool_routing/param_correction/multi_step_flow/business_knowledge/user_preference/api_behavior_patch/conversation_pattern',
  `learn_key` varchar(200) NOT NULL COMMENT '学习记录唯一键，如 tool_routing:erp_material_query',
  `content` json NOT NULL COMMENT '学习内容(JSON格式)，结构因learn_type不同而不同',
  `confidence` decimal(3,2) DEFAULT 0.50 COMMENT '置信度：0.00-1.00，初始0.5，命中/验证后提升',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态：pending(待验证)/active(已生效)/rejected(已拒绝)/expired(已过期)',
  `source_type` varchar(20) NOT NULL COMMENT '来源：user_correction(用户纠正)/agent_discovery(自动发现)/admin_input(管理员输入)',
  `source_session_id` varchar(100) DEFAULT NULL COMMENT '来源会话ID',
  `source_message_ids` json DEFAULT NULL COMMENT '来源消息ID列表',
  `effective_scope` varchar(20) DEFAULT 'global' COMMENT '生效范围：global(全局)/tenant(租户)/user(用户)',
  `scope_id` bigint unsigned DEFAULT NULL COMMENT '作用域ID：tenant→shopid, user→userid',
  `applicable_tools` json DEFAULT NULL COMMENT '适用的工具名列表，用于过滤注入',
  `trigger_patterns` json DEFAULT NULL COMMENT '触发关键词/意图模式列表',
  `hit_count` int DEFAULT 0 COMMENT '命中次数（被注入且用户未纠正）',
  `last_hit_time` datetime DEFAULT NULL COMMENT '最近命中时间',
  `verified` tinyint(1) DEFAULT 0 COMMENT '是否经过人工验证：0未验证/1已验证',
  `verified_by` bigint unsigned DEFAULT NULL COMMENT '验证人',
  `verified_time` datetime DEFAULT NULL COMMENT '验证时间',
  `expires_at` datetime DEFAULT NULL COMMENT '过期时间(NULL表示永不过期)',
  `operator` bigint unsigned DEFAULT NULL COMMENT '创建人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `createtime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `opttime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_learn_key` (`learn_key`),
  KEY `idx_learn_type` (`learn_type`),
  KEY `idx_status` (`status`),
  KEY `idx_scope` (`effective_scope`, `scope_id`),
  KEY `idx_confidence` (`confidence` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent自我学习记录表';

-- Agent学习反馈表：记录学习记录的命中和反馈，用于置信度调整
CREATE TABLE IF NOT EXISTS `t_sys_agent_learning_feedback` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `learning_id` bigint unsigned NOT NULL COMMENT '关联的学习记录ID',
  `session_id` varchar(100) NOT NULL COMMENT '会话ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `feedback_type` varchar(20) NOT NULL COMMENT '反馈类型：positive(正确)/negative(错误)/neutral(无感)',
  `feedback_detail` varchar(500) DEFAULT NULL COMMENT '反馈详情',
  `createtime` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_learning_id` (`learning_id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent学习反馈表';

-- =============================================
-- 种子数据：预置的学习记录（由管理员验证，直接生效）
-- =============================================

-- 工具选择纠正：查询ERP物料必须用material_list
INSERT INTO `t_sys_agent_learning` (`learn_type`, `learn_key`, `content`, `confidence`, `status`, `source_type`, `effective_scope`, `applicable_tools`, `trigger_patterns`, `verified`, `remark`) VALUES
('tool_routing', 'tool_routing:erp_material_query', '{"intent_pattern":"查询ERP物料/产品/商品信息","wrong_tool":"amz_query_product_info_list,amz_query_product_list,amz_query_product_by_sku","correct_tool":"material_list","reason":"ERP物料查询必须使用material_list，amz_开头的工具仅用于Amazon商品查询"}', 0.99, 'active', 'admin_input', 'global', '["material_list"]', '["物料","产品","商品","SKU查询","ERP商品"]', 1, 'ERP域与Amazon域工具选择规则，管理员验证通过');

-- 参数纠正：material_list查询正常物料必须传isDelete=0
INSERT INTO `t_sys_agent_learning` (`learn_type`, `learn_key`, `content`, `confidence`, `status`, `source_type`, `effective_scope`, `applicable_tools`, `trigger_patterns`, `verified`, `remark`) VALUES
('param_correction', 'param_correction:material_list_isdelete', '{"tool_name":"material_list","param_name":"isDelete","correct_param":"0","wrong_param":"不传或为空","reason":"查询正常物料时必须传isDelete=0，否则可能返回空数据或归档数据"}', 0.99, 'active', 'admin_input', 'global', '["material_list"]', '["查询物料","物料列表","产品查询"]', 1, 'material_list的isDelete参数默认值问题');

-- 工具选择纠正：查询ERP库存必须用inv_list
INSERT INTO `t_sys_agent_learning` (`learn_type`, `learn_key`, `content`, `confidence`, `status`, `source_type`, `effective_scope`, `applicable_tools`, `trigger_patterns`, `verified`, `remark`) VALUES
('tool_routing', 'tool_routing:erp_inventory_query', '{"intent_pattern":"查询ERP库存信息","wrong_tool":"amz_query_fba_inventory","correct_tool":"inv_list","reason":"ERP库存查询使用inv_list，amz_query_fba_inventory仅用于Amazon FBA库存查询"}', 0.99, 'active', 'admin_input', 'global', '["inv_list","inv_list_turnover"]', '["库存","ERP库存","仓库库存"]', 1, 'ERP库存与FBA库存工具选择规则');

-- 工具选择纠正：查询采购单必须用purchase_list
INSERT INTO `t_sys_agent_learning` (`learn_type`, `learn_key`, `content`, `confidence`, `status`, `source_type`, `effective_scope`, `applicable_tools`, `trigger_patterns`, `verified`, `remark`) VALUES
('tool_routing', 'tool_routing:erp_purchase_query', '{"intent_pattern":"查询采购单信息","wrong_tool":"amz_开头的工具","correct_tool":"purchase_list","reason":"采购单查询必须使用purchase_list，这是ERP域工具"}', 0.99, 'active', 'admin_input', 'global', '["purchase_list","purchase_list_details"]', '["采购","采购单","供应商采购"]', 1, 'ERP采购域工具选择规则');
