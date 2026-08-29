-- ============================================
-- plantool 计划工具模块菜单SQL
-- 说明: 将旧plantool项目的菜单迁移到新前后端分离架构
-- 视图路径对应: src/views/customized/plantool/xxx/index.vue
-- 执行方式: 在db_admin数据库中执行
-- ============================================

-- 1. 创建二级菜单: 计划工具 (parent_id=48 为"订制"顶级菜单)
-- 使用 INSERT IGNORE 避免重复插入
INSERT IGNORE INTO `t_sys_menu`
(`id`, `name`, `parent_id`, `apppath`, `path`, `component`, `icon`, `appicon`, `sort`, `visible`, `redirect`, `runui`, `runapp`, `gmt_create`, `gmt_modified`, `oldid`)
VALUES
(60001, '计划工具', 48, NULL, NULL, NULL, 'DataAnalysis', '', 720, 1, '', b'1', NULL, NOW(), NOW(), NULL);

-- 获取计划工具菜单ID (用于后续设置parent_id)
-- 如果上面INSERT IGNORE跳过了，说明id=60001已存在，直接使用

-- 2. 叶子菜单: 销售月报
-- oldid: 9f8895d5-f045-11eb-a2c5-00e04c023f0e
INSERT INTO `t_sys_menu`
(`id`, `name`, `parent_id`, `apppath`, `path`, `component`, `icon`, `appicon`, `sort`, `visible`, `redirect`, `runui`, `runapp`, `gmt_create`, `gmt_modified`, `oldid`)
VALUES
(60002, '销售月报', 60001, NULL, '/customized/plantool/salemonth', 'customized/plantool/salemonth/index', NULL, '', 711, 1, '', b'1', NULL, NOW(), NOW(), '9f8895d5-f045-11eb-a2c5-00e04c023f0e')
ON DUPLICATE KEY UPDATE
  `parent_id` = 60001,
  `apppath` = NULL,
  `path` = '/customized/plantool/salemonth',
  `component` = 'customized/plantool/salemonth/index',
  `visible` = 1,
  `redirect` = '',
  `runui` = b'1',
  `gmt_modified` = NOW();

-- 3. 叶子菜单: 出货计划
-- oldid: 147ff39c-e9f9-11eb-a844-00e04c023f0e
INSERT INTO `t_sys_menu`
(`id`, `name`, `parent_id`, `apppath`, `path`, `component`, `icon`, `appicon`, `sort`, `visible`, `redirect`, `runui`, `runapp`, `gmt_create`, `gmt_modified`, `oldid`)
VALUES
(60003, '出货计划', 60001, NULL, '/customized/plantool/shipplan', 'customized/plantool/shipplan/index', NULL, '', 713, 1, '', b'1', NULL, NOW(), NOW(), '147ff39c-e9f9-11eb-a844-00e04c023f0e')
ON DUPLICATE KEY UPDATE
  `parent_id` = 60001,
  `apppath` = NULL,
  `path` = '/customized/plantool/shipplan',
  `component` = 'customized/plantool/shipplan/index',
  `visible` = 1,
  `redirect` = '',
  `runui` = b'1',
  `gmt_modified` = NOW();

-- 4. 叶子菜单: 人力计划
-- oldid: 43741840-edbe-11eb-a2c5-00e04c023f0e
INSERT INTO `t_sys_menu`
(`id`, `name`, `parent_id`, `apppath`, `path`, `component`, `icon`, `appicon`, `sort`, `visible`, `redirect`, `runui`, `runapp`, `gmt_create`, `gmt_modified`, `oldid`)
VALUES
(60004, '人力计划', 60001, NULL, '/customized/plantool/manplan', 'customized/plantool/manplan/index', NULL, '', 714, 1, '', b'1', NULL, NOW(), NOW(), '43741840-edbe-11eb-a2c5-00e04c023f0e')
ON DUPLICATE KEY UPDATE
  `parent_id` = 60001,
  `apppath` = NULL,
  `path` = '/customized/plantool/manplan',
  `component` = 'customized/plantool/manplan/index',
  `visible` = 1,
  `redirect` = '',
  `runui` = b'1',
  `gmt_modified` = NOW();

-- 5. 叶子菜单: 物料需求
-- oldid: b3d41e98-edda-11eb-a2c5-00e04c023f0e
INSERT INTO `t_sys_menu`
(`id`, `name`, `parent_id`, `apppath`, `path`, `component`, `icon`, `appicon`, `sort`, `visible`, `redirect`, `runui`, `runapp`, `gmt_create`, `gmt_modified`, `oldid`)
VALUES
(60005, '物料需求', 60001, NULL, '/customized/plantool/purchaseplan', 'customized/plantool/purchaseplan/index', NULL, '', 715, 1, '', b'1', NULL, NOW(), NOW(), 'b3d41e98-edda-11eb-a2c5-00e04c023f0e')
ON DUPLICATE KEY UPDATE
  `parent_id` = 60001,
  `apppath` = NULL,
  `path` = '/customized/plantool/purchaseplan',
  `component` = 'customized/plantool/purchaseplan/index',
  `visible` = 1,
  `redirect` = '',
  `runui` = b'1',
  `gmt_modified` = NOW();

-- 6. 叶子菜单: 采购审核
-- oldid: 0110555c-f05f-11eb-a2c5-00e04c023f0e
INSERT INTO `t_sys_menu`
(`id`, `name`, `parent_id`, `apppath`, `path`, `component`, `icon`, `appicon`, `sort`, `visible`, `redirect`, `runui`, `runapp`, `gmt_create`, `gmt_modified`, `oldid`)
VALUES
(60006, '采购审核', 60001, NULL, '/customized/plantool/purchaseplan/formlist', 'customized/plantool/purchaseplan/index', NULL, '', 716, 1, '', b'1', NULL, NOW(), NOW(), '0110555c-f05f-11eb-a2c5-00e04c023f0e')
ON DUPLICATE KEY UPDATE
  `parent_id` = 60001,
  `apppath` = NULL,
  `path` = '/customized/plantool/purchaseplan/formlist',
  `component` = 'customized/plantool/purchaseplan/index',
  `visible` = 1,
  `redirect` = '',
  `runui` = b'1',
  `gmt_modified` = NOW();

-- 7. 叶子菜单: 提货付款
-- oldid: 880d01f2-f32e-11eb-a2c5-00e04c023f0e
INSERT INTO `t_sys_menu`
(`id`, `name`, `parent_id`, `apppath`, `path`, `component`, `icon`, `appicon`, `sort`, `visible`, `redirect`, `runui`, `runapp`, `gmt_create`, `gmt_modified`, `oldid`)
VALUES
(60007, '提货付款', 60001, NULL, '/customized/plantool/pickpay', 'customized/plantool/pickpay/index', NULL, '', 717, 1, '', b'1', NULL, NOW(), NOW(), '880d01f2-f32e-11eb-a2c5-00e04c023f0e')
ON DUPLICATE KEY UPDATE
  `parent_id` = 60001,
  `apppath` = NULL,
  `path` = '/customized/plantool/pickpay',
  `component` = 'customized/plantool/pickpay/index',
  `visible` = 1,
  `redirect` = '',
  `runui` = b'1',
  `gmt_modified` = NOW();

-- ============================================
-- 验证脚本 (可选执行)
-- ============================================
-- SELECT id, name, parent_id, path, component, visible
-- FROM t_sys_menu
-- WHERE parent_id = 60001 OR id = 60001
-- ORDER BY sort;
