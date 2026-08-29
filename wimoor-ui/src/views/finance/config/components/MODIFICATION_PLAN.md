# 财务ERP进销存模块前端修改文档

## 一、文档概述

本文档基于 [财务ERP进销存模块详细开发流程文档](https://wiki.wimoor.com/books/d61e4/page/erp) 与现有系统结构的对比分析，明确前端需要新增和修改的内容。

---

## 二、现有系统结构分析

### 2.1 已有数据表映射

| 文档表名 | 系统现有表 | 说明 | 状态 |
|---------|-----------|------|------|
| `fin_purchase_account` | `t_erp_fin_account` | 采购账户表（贷方） | ✅ 已有，需扩展 |
| 费用类型 | `t_erp_fin_project` | 流水账类型（借方） | ✅ 已有 |
| `fin_purchase_order` | `t_erp_purchase_form_entry` | 采购订单明细表 | ✅ 已有，需扩展 |
| `fin_purchase_payment` | `t_erp_purchase_form_payment` | 采购付款表 | ✅ 已有，需扩展 |
| `fin_invoice` | - | 发票台账表 | ❌ 需新建 |
| `fin_warehouse_stock` | `t_erp_inventory` | 库存表 | ✅ 已有，需扩展 |
| `fin_inventory_transaction` | `t_erp_inventory_record` | 库存变动记录表 | ✅ 已有，需扩展 |
| `fin_supplier_bank` | - | 供应商收款账户 | ❌ 需新建 |

**复用分析详情**：

#### `t_erp_purchase_form_entry` → `fin_purchase_order`
| 文档字段 | 系统字段 | 说明 |
|---------|---------|------|
| supplier_id | `supplier` | ✅ 已有 |
| purchase_account_id | - | ❌ 需新增 |
| sku | `materialid` | ✅ 已有 |
| warehouse_code | `formid`关联`t_erp_purchase_form.warehouseid` | ✅ 已有 |
| order_qty | `amount` | ✅ 已有 |
| unit_price | `itemprice` | ✅ 已有 |
| total_amount | `orderprice` | ✅ 已有 |
| paid_amount | `totalpay` | ✅ 已有 |
| status_order | `auditstatus` | ✅ 已有（0草稿,1待审核,2审核通过,3已完成） |
| status_pay | `paystatus` | ✅ 已有 |
| status_inv | - | ❌ 需新增 |
| status_stock | `inwhstatus` | ✅ 已有 |

#### `t_erp_purchase_form_payment` → `fin_purchase_payment`
| 文档字段 | 系统字段 | 说明 |
|---------|---------|------|
| purchase_order_id | `formentryid` | ✅ 已有 |
| purchase_account_id | `acct` | ✅ 已有 |
| fee_type | `projectid` | ✅ 已有（关联费用类型） |
| amount | `payprice` | ✅ 已有 |
| payment_date | `opttime` | ✅ 已有 |
| voucher_id | - | ❌ 需新增 |
| is_refund | - | ❌ 需新增 |
| refund_original_id | - | ❌ 需新增 |

#### `t_erp_inventory` → `fin_warehouse_stock`
| 文档字段 | 系统字段 | 说明 |
|---------|---------|------|
| sku | `materialid` | ✅ 已有 |
| warehouse_code | `warehouseid` | ✅ 已有 |
| current_qty | `quantity` | ✅ 已有 |
| current_amount | - | ❌ 需新增（金额） |
| unit_cost | - | ❌ 需新增（移动平均单价） |
| period | - | ❌ 需新增（会计期间） |

#### `t_erp_inventory_record` → `fin_inventory_transaction`
| 文档字段 | 系统字段 | 说明 |
|---------|---------|------|
| sku | `materialid` | ✅ 已有 |
| warehouse_code | `warehouseid` | ✅ 已有 |
| trans_date | `opttime` | ✅ 已有 |
| trans_type | `formtype` | ✅ 已有 |
| source_doc_id | `formid`/`number` | ✅ 已有 |
| qty_change | `quantity` | ✅ 已有 |
| amount_change | - | ❌ 需新增（金额变动） |
| unit_cost | - | ❌ 需新增（发生时单价） |
| voucher_id | - | ❌ 需新增（关联凭证） |

### 2.2 已有功能模块

| 模块路径 | 功能 | 状态 |
|---------|------|------|
| `erp/finance/account/index.vue` | 采购账户管理（t_erp_fin_account） | ✅ 已有 |
| `api/erp/finance/project.js` | 费用类型/账户API | ✅ 已有 |
| `finance/config/erpconfig/payconfig.vue` | 付款凭证映射规则配置 | ✅ 已有 |
| `finance/config/erpconfig/inventoryconfig.vue` | 在途库存凭证映射规则配置 | ✅ 已有 |
| `finance/subjects/index.vue` | 会计科目管理 | ✅ 已有 |
| `erp/purchase/orders/` | 采购订单管理 | ✅ 已有 |
| `finance/vouchers/` | 凭证管理 | ✅ 已有 |
| `finance/ledger/` | 账簿管理 | ✅ 已有 |

### 2.3 API层现状

| API文件 | 功能 |
|---------|------|
| `api/erp/finance/project.js` | 费用类型（getProject）、采购账户（getAccountAll） |
| `api/erp/finances/faccountApi.js` | 采购账户CRUD |
| `api/finance/mappingErpAccount.js` | 费用类型-科目映射规则 |
| `api/finance/mappingErpFeetype.js` | 采购账户-科目映射规则 |
| `api/finance/mappingErpInventory.js` | 存货映射规则 |
| `api/finance/subjects.js` | 会计科目 |
| `api/finance/vouchers.js` | 凭证管理 |

---

## 三、功能模块开发清单

### 3.1 阶段一：基础配置模块

#### 3.1.1 采购账户管理（扩展现有）

**现有页面**: `erp/finance/account/index.vue` ✅ 已有
**现有API**: `api/erp/finances/faccountApi.js` ✅ 已有

**现状分析**:
- 已有 `t_erp_fin_account` 表，支持账户CRUD
- 已有账户余额管理、默认账户设置
- 已有支付方式（paymeth）关联

**需要扩展的功能**:

| 扩展项 | 说明 | 工作量 |
|-------|------|--------|
| 关联会计科目 | 增加 `relatedSubjectId` 字段，关联 `fin_accounting_subjects` | 小 |
| 账户类型扩展 | 区分现金类/账期类账户 | 小 |
| 配置JSON管理 | 增加费用类型科目映射配置 | 中 |
| 启用/停用状态 | 增加 `is_enabled` 字段 | 小 |

**改造方案**:
1. 在现有账户编辑弹窗中增加"会计科目关联"字段
2. 增加账户类型选择（现金/账期）
3. 增加配置JSON编辑区域（可折叠面板）

**新增字段映射**:
| 文档字段 | 现有字段 | 需新增 |
|---------|---------|--------|
| account_name | `name` | - |
| account_type | - | ✅ 需新增 |
| related_subject_id | - | ✅ 需新增 |
| config_json | - | ✅ 需新增 |
| is_enabled | - | ✅ 需新增 |

---

#### 3.1.2 供应商收款账户管理（新增）

**页面路径**: `erp/baseinfo/supplier/components/bank-account.vue`

**功能需求**:
- 供应商收款账户CRUD
- 支持多账户，设置默认账户
- 与现有供应商管理页面集成

**新增API**: `api/erp/supplierBank.js`

```javascript
// 接口清单
POST /api/finance/supplier/bank/create      // 创建收款账户
POST /api/finance/supplier/bank/update      // 更新收款账户
DELETE /api/finance/supplier/bank/delete    // 删除收款账户
GET /api/finance/supplier/bank/list         // 查询列表（按供应商）
POST /api/finance/supplier/bank/setDefault  // 设置默认账户
```

**核心字段**:
| 字段 | 说明 | 类型 |
|-----|------|------|
| supplierId | 供应商ID | Number |
| bankName | 开户银行 | String |
| accountName | 收款账户名称 | String |
| accountNo | 收款账号 | String |
| isDefault | 是否默认 | Number |

---

#### 3.1.3 进销存全局配置（新增）

**页面路径**: `finance/config/inventory-global/index.vue`

**功能需求**:
- 成本核算方法配置（仅支持移动加权平均）
- 其他全局配置项

**新增API**: `api/finance/inventoryConfig.js`

```javascript
// 接口清单
GET /api/finance/inventory/config/get     // 获取配置
POST /api/finance/inventory/config/update // 更新配置
```

---

### 3.2 阶段二：采购订单（会计版）

#### 3.2.1 采购订单页面改造

**现有页面**: `erp/purchase/orders/index.vue` ✅ 已有
**现有组件**:
- `components/create.vue` - 订单创建
- `components/payment.vue` - 付款
- `components/receipt.vue` - 收货
- `components/details.vue` - 详情

**需要新增的功能**:

1. **订单状态机展示**（扩展现有表格列）
   - 订单状态（status_order）：草稿→已确认→已完成/已取消
   - 付款状态（status_pay）：未付款→部分付款→已付款→退款中→已退款
   - 库存状态（status_stock）：未收货→部分收货→已收货
   - 发票状态（status_inv）：未开票→部分开票→已开票

2. **会计版订单创建表单**（改造现有组件）
   **改造文件**: `erp/purchase/orders/components/create.vue`
   
   新增字段：
   - purchaseAccountId（采购账户选择）- 调用 `getAccountAll()`
   - 选择费用类型（GOODS/FREIGHT/SERVICE）- 调用 `getProject()`
   - 关联会计科目预览

3. **付款弹窗改造**
   **改造文件**: `erp/purchase/orders/components/payment.vue`
   
   新增功能：
   - 选择采购账户
   - 选择费用类型
   - 自动计算凭证借贷科目（基于映射规则）
   - 凭证预览

4. **入库凭证生成**
   **改造文件**: `erp/purchase/orders/components/receipt.vue`
   
   新增功能：
   - 入库时自动生成凭证
   - 移动加权平均单价计算
   - 凭证预览与确认

**新增API**: `api/finance/purchaseOrder.js`

```javascript
// 接口清单
POST /api/finance/purchase/order/create    // 创建订单（会计版）
POST /api/finance/purchase/order/confirm   // 确认订单
POST /api/finance/purchase/order/pay       // 付款（生成凭证）
POST /api/finance/purchase/order/refund    // 退款（生成红字凭证）
POST /api/finance/purchase/order/receive   // 入库（生成凭证）
GET /api/finance/purchase/order/list       // 查询列表
GET /api/finance/purchase/order/detail     // 查询详情
```

---

### 3.3 阶段三：采购账户台账

#### 3.3.1 采购账户台账设计（详细）

**需求来源**: 原始需求中"采购账户台账：账期余额/对账/还款，支持上传账期对账单"

**页面路径**: `finance/purchase-ledger/index.vue`

**数据来源**:
- `t_erp_fin_account` - 账户信息
- `t_erp_purchase_form_entry` - 采购订单
- `t_erp_purchase_form_payment` - 付款记录
- `t_erp_purchase_form` - 订单主表

**台账字段设计**:

```
采购账户台账 = 账户维度 + 订单维度 + 付款维度 + 对账维度

账户维度（左侧卡片）：
├── account_id          // 账户ID
├── account_name        // 账户名称
├── account_type        // 账户类型（1现金/2账期）
├── balance             // 当前余额
├── total_order_amount  // 订单总额
├── total_paid_amount   // 已付总额
└── total_unpaid_amount // 未付总额

订单维度（主表格）：
├── order_id            // 订单ID（t_erp_purchase_form_entry.id）
├── order_number        // 订单号（t_erp_purchase_form.number）
├── supplier_name       // 供应商名称
├── material_name       // 物料名称
├── material_sku        // SKU
├── order_amount        // 订单金额（orderprice）
├── paid_amount         // 已付金额（totalpay）
├── unpaid_amount       // 未付金额（orderprice - totalpay）
├── status_order        // 订单状态（auditstatus）
├── status_pay          // 付款状态（paystatus）
├── createdate          // 创建时间
└── deliverydate        // 交货日期

付款维度（展开行/抽屉）：
├── payment_id          // 付款ID
├── payment_amount      // 付款金额（payprice）
├── payment_date        // 付款时间（opttime）
├── project_name        // 费用类型（projectid关联t_erp_fin_project）
├── voucher_id          // 凭证ID（需新增）
└── remark              // 备注

对账维度（弹窗）：
├── reconcile_id        // 对账单ID
├── reconcile_date      // 对账日期
├── reconcile_amount    // 对账金额
├── reconcile_status    // 对账状态
└── attachment_url      // 对账单附件
```

**页面组件结构**:
```
finance/purchase-ledger/
├── index.vue                    // 主页面
└── components/
    ├── header.vue               // 查询头部
    ├── account-cards.vue        // 账户卡片列表（左侧）
    ├── order-table.vue          // 订单明细表格（右侧）
    ├── payment-drawer.vue       // 付款明细抽屉
    ├── pay-dialog.vue           // 付款弹窗（支持一笔付多单）
    ├── reconcile-dialog.vue     // 对账弹窗
    ├── upload-dialog.vue        // 批量上传对账单
    └── statistics.vue           // 统计图表
```

**核心功能**:

1. **账户卡片展示**
   - 左侧展示所有采购账户卡片
   - 点击账户切换台账数据
   - 显示账户余额、订单总额、已付/未付金额

2. **订单明细查询**
   - 多维度筛选：供应商、日期范围、状态
   - 支持批量选择订单进行付款
   - 展开行显示付款明细

3. **台账付款**
   - 支持一笔付款关联多笔订单（账期场景）
   - 选择费用类型、填写金额
   - 自动生成凭证

4. **对账单管理**
   - 批量上传Excel对账单
   - 自动匹配订单
   - 差异标记

**新增API**: `api/finance/purchaseLedger.js`

```javascript
// 接口清单
GET /api/finance/purchase/ledger/accounts        // 账户列表（带余额统计）
GET /api/finance/purchase/ledger/list             // 台账列表
POST /api/finance/purchase/ledger/pay             // 台账付款（支持一笔付多单）
POST /api/finance/purchase/ledger/upload          // 批量上传对账单
GET /api/finance/purchase/ledger/statistics       // 统计数据
GET /api/finance/purchase/ledger/export           // 导出
POST /api/finance/purchase/ledger/reconcile       // 对账操作
```

---

### 3.4 阶段四：发票台账与供应商台账

#### 3.4.1 发票台账设计（详细）

**需求来源**: 原始需求中"发票台账：发票-采购订单-付款-税-供应商全链条关联，支持境内/境外发票"

**页面路径**: `finance/invoice-ledger/index.vue`

**新建表**: `fin_invoice`

```sql
CREATE TABLE fin_invoice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  invoice_no VARCHAR(100) COMMENT '发票号码',
  invoice_type VARCHAR(20) COMMENT '发票类型：DIGITAL_VAT/ DIGITAL_NORMAL/TRADITIONAL/OVERSEAS',
  groupid BIGINT COMMENT '账套ID',
  supplier_id BIGINT COMMENT '供应商ID',
  seller_name VARCHAR(200) COMMENT '销方名称',
  seller_tax_no VARCHAR(50) COMMENT '销方税号',
  invoice_date DATE COMMENT '开票日期',
  amount_with_tax DECIMAL(18,2) COMMENT '价税合计',
  amount_without_tax DECIMAL(18,2) COMMENT '不含税金额',
  tax_amount DECIMAL(18,2) COMMENT '税额',
  currency VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
  exchange_rate DECIMAL(18,6) DEFAULT 1.0 COMMENT '汇率',
  status VARCHAR(20) COMMENT '状态：NORMAL/CANCELLED/RED_ALL/RED_PART/ABNORMAL',
  posting_status TINYINT DEFAULT 0 COMMENT '入账状态：0未入账/1已入账',
  voucher_id BIGINT COMMENT '入账凭证ID',
  purchase_order_ids VARCHAR(500) COMMENT '关联采购订单ID（JSON数组）',
  payment_ids VARCHAR(500) COMMENT '关联付款ID（JSON数组）',
  attachment_urls TEXT COMMENT '附件URL（JSON数组）',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_invoice_no (invoice_no),
  INDEX idx_supplier_id (supplier_id),
  INDEX idx_invoice_date (invoice_date)
) COMMENT='发票台账表';
```

**台账字段设计**:

```
发票台账 = 发票信息 + 关联单据 + 入账状态

发票信息：
├── invoice_id          // 发票ID
├── invoice_no          // 发票号码
├── invoice_type        // 发票类型（数电专票/数电普票/传统/境外）
├── invoice_date        // 开票日期
├── supplier_name       // 供应商名称
├── seller_name         // 销方名称
├── seller_tax_no       // 销方税号
├── amount_with_tax     // 价税合计
├── amount_without_tax  // 不含税金额
├── tax_amount          // 税额
├── currency            // 币种
├── exchange_rate       // 汇率
├── status              // 状态（正常/作废/红冲全部/红冲部分/异常）
└── posting_status      // 入账状态（未入账/已入账）

关联单据（展开行/抽屉）：
├── purchase_order_ids  // 关联采购订单
├── purchase_order_numbers // 采购订单号列表
├── payment_ids         // 关联付款记录
└── payment_amounts     // 付款金额列表

入账信息：
├── voucher_id          // 凭证ID
├── voucher_number      // 凭证字号
├── posting_date        // 入账日期
└── posting_amount      // 入账金额
```

**页面组件结构**:
```
finance/invoice-ledger/
├── index.vue                    // 主页面
└── components/
    ├── header.vue               // 查询头部
    ├── invoice-table.vue        // 发票列表表格
    ├── sync-dialog.vue          // 同步弹窗（从税局API获取）
    ├── posting-dialog.vue       // 入账弹窗（生成凭证）
    ├── detail-drawer.vue        // 发票详情抽屉
    ├── relation-drawer.vue      // 关联单据抽屉
    └── import-dialog.vue        // 导入弹窗（手动导入）
```

**核心功能**:

1. **发票同步**
   - 从税局API批量获取发票数据
   - 自动匹配供应商（根据销方税号）
   - 自动关联采购订单（根据金额、供应商匹配）

2. **发票入账**
   - 选择发票批量入账
   - 自动生成凭证：
     ```
     借：预付账款-ERP在途发票
     贷：预付账款-ERP采购供应商
     ```
   - 关联凭证ID到发票记录

3. **全链条关联查询**
   - 发票→采购订单→付款→供应商
   - 支持反向查询：从付款查发票

4. **发票状态管理**
   - 正常/作废/红冲状态管理
   - 异常发票标记

**新增API**: `api/finance/invoiceLedger.js`

```javascript
// 接口清单
GET /api/finance/invoice/list           // 发票列表
POST /api/finance/invoice/sync          // 同步发票（从税局API）
POST /api/finance/invoice/import        // 导入发票（手动）
POST /api/finance/invoice/posting       // 发票入账（生成凭证）
GET /api/finance/invoice/detail         // 发票详情
GET /api/finance/invoice/relations      // 关联单据查询
GET /api/finance/invoice/export         // 导出
```

---

#### 3.4.2 供应商台账设计（详细）

**需求来源**: 原始需求中"供应商台账：采购/物流对账单<>发票台账，供应商维度汇总"

**页面路径**: `finance/supplier-ledger/index.vue`

**数据来源**:
- `t_erp_material_supplier` - 供应商信息
- `t_erp_purchase_form_entry` - 采购订单
- `t_erp_purchase_form_payment` - 付款记录
- `fin_invoice` - 发票台账（新建）

**台账字段设计**:

```
供应商台账 = 供应商维度聚合（采购+付款+发票）

供应商汇总维度：
├── supplier_id           // 供应商ID
├── supplier_name         // 供应商名称
├── contact_person        // 联系人
├── phone                 // 联系电话
│
├── 采购汇总
│   ├── total_order_count     // 订单总数
│   ├── total_order_amount    // 订单总额
│   ├── total_received_amount // 已收货金额
│   └── total_return_amount   // 退货金额
│
├── 付款汇总
│   ├── total_paid_amount     // 已付总额
│   ├── total_unpaid_amount   // 未付总额
│   └── total_refund_amount   // 退款总额
│
├── 发票汇总
│   ├── total_invoiced_amount // 已开票总额
│   ├── total_uninvoiced_amount // 未开票总额
│   └── total_tax_amount      // 税额合计
│
└── 对账状态
    ├── last_reconcile_date   // 最后对账日期
    └── reconcile_status      // 对账状态

供应商明细维度（展开/抽屉）：
├── 订单明细列表
│   ├── order_id
│   ├── order_number
│   ├── order_amount
│   ├── paid_amount
│   └── status
│
├── 付款明细列表
│   ├── payment_id
│   ├── payment_amount
│   ├── payment_date
│   └── voucher_number
│
└── 发票明细列表
    ├── invoice_id
    ├── invoice_no
    ├── invoice_amount
    └── posting_status
```

**页面组件结构**:
```
finance/supplier-ledger/
├── index.vue                    // 主页面
└── components/
    ├── header.vue               // 查询头部
    ├── summary-table.vue        // 供应商汇总表格
    ├── detail-drawer.vue        // 供应商明细抽屉
    ├── order-tab.vue            // 订单明细标签页
    ├── payment-tab.vue          // 付款明细标签页
    ├── invoice-tab.vue          // 发票明细标签页
    └── export-dialog.vue        // 导出弹窗
```

**核心功能**:

1. **供应商汇总查询**
   - 按供应商维度聚合数据
   - 显示采购/付款/发票汇总
   - 支持筛选：供应商名称、对账状态

2. **明细穿透查询**
   - 点击供应商展开明细
   - 标签页切换：订单/付款/发票
   - 支持从发票反查订单、付款

3. **未开票订单导出**
   - 导出未开票的采购订单
   - 便于与供应商对账

4. **对账功能**
   - 标记对账状态
   - 记录对账日期

**新增API**: `api/finance/supplierLedger.js`

```javascript
// 接口清单
GET /api/finance/supplier/ledger/summary    // 供应商汇总
GET /api/finance/supplier/ledger/orders     // 供应商订单明细
GET /api/finance/supplier/ledger/payments   // 供应商付款明细
GET /api/finance/supplier/ledger/invoices   // 供应商发票明细
GET /api/finance/supplier/ledger/export     // 导出未开票订单
POST /api/finance/supplier/ledger/reconcile // 标记对账
```

---

### 3.5 阶段五：进销存台账（本地仓库）

#### 3.5.1 进销存台账设计（详细）

**需求来源**: 原始需求中"进销存台账：采购>付款>入库>出库全流程，SKU-库位-库存按唯一账簿归属"

**页面路径**: `finance/inventory-ledger/index.vue`

**数据来源**:
- `t_erp_inventory` - 库存余额（需扩展金额字段）
- `t_erp_inventory_record` - 库存变动记录（需扩展金额字段）
- `t_erp_purchase_form_entry` - 采购订单
- `t_erp_purchase_form_receive` - 采购收货
- `t_erp_inwh_form` - 入库单
- `t_erp_outwh_form` - 出库单

**表结构扩展**:

```sql
-- t_erp_inventory 扩展字段
ALTER TABLE t_erp_inventory ADD COLUMN current_amount DECIMAL(18,2) DEFAULT 0 COMMENT '当前金额';
ALTER TABLE t_erp_inventory ADD COLUMN unit_cost DECIMAL(18,4) DEFAULT 0 COMMENT '移动平均单价';
ALTER TABLE t_erp_inventory ADD COLUMN period VARCHAR(6) COMMENT '会计期间（YYYYMM）';

-- t_erp_inventory_record 扩展字段
ALTER TABLE t_erp_inventory_record ADD COLUMN amount_change DECIMAL(18,2) DEFAULT 0 COMMENT '金额变动';
ALTER TABLE t_erp_inventory_record ADD COLUMN unit_cost DECIMAL(18,4) DEFAULT 0 COMMENT '发生时单价';
ALTER TABLE t_erp_inventory_record ADD COLUMN voucher_id BIGINT COMMENT '关联凭证ID';
```

**台账字段设计**:

```
进销存台账 = 汇总账 + 明细账 + 勾稽校验

汇总账维度：
├── sku                   // SKU编码（materialid）
├── material_name         // 物料名称
├── warehouse_id          // 仓库ID
├── warehouse_name        // 仓库名称
├── warehouse_type        // 仓库类型（1本地/2FBA/3海外仓）
│
├── 库存数据
│   ├── current_qty         // 当前数量
│   ├── current_amount      // 当前金额
│   ├── unit_cost           // 移动平均单价
│   └── period              // 会计期间
│
├── 本期变动
│   ├── in_qty              // 本期入库数量
│   ├── in_amount           // 本期入库金额
│   ├── out_qty             // 本期出库数量
│   └── out_amount          // 本期出库金额
│
└── 累计数据
    ├── total_in_qty        // 累计入库数量
    ├── total_in_amount     // 累计入库金额
    ├── total_out_qty       // 累计出库数量
    └── total_out_amount    // 累计出库金额

明细账维度：
├── trans_date            // 变动日期（opttime）
├── trans_type            // 变动类型（formtype）
│   ├── PURCHASE_IN       // 采购入库
│   ├── SALE_OUT          // 销售出库
│   ├── TRANSFER_IN       // 调拨入库
│   ├── TRANSFER_OUT      // 调拨出库
│   ├── ADJUST            // 盘点调整
│   └── OTHER             // 其他
├── source_doc_id         // 来源单据ID（formid）
├── source_doc_number     // 来源单据号（number）
│
├── 数量变动
│   ├── qty_before        // 变动前数量（startfulfillable）
│   ├── qty_change        // 变动数量（quantity，正为入库负为出库）
│   └── qty_after         // 变动后数量（endfulfillable）
│
├── 金额变动
│   ├── amount_before     // 变动前金额
│   ├── amount_change     // 变动金额
│   └── amount_after      // 变动后金额
│
├── 单价信息
│   ├── unit_cost         // 发生时单价
│   └── total_cost        // 总金额（qty_change × unit_cost）
│
└── 凭证信息
    ├── voucher_id        // 凭证ID
    └── voucher_number    // 凭证字号

勾稽校验维度：
├── sku                   // SKU
├── warehouse_id          // 仓库
├── inventory_qty         // 库存表数量
├── record_sum_qty        // 记录表汇总数量
├── diff_qty              // 差异数量
├── inventory_amount      // 库存表金额
├── record_sum_amount     // 记录表汇总金额
├── diff_amount           // 差异金额
└── check_status          // 校验状态（正常/异常）
```

**页面组件结构**:
```
finance/inventory-ledger/
├── index.vue                    // 主页面
└── components/
    ├── header.vue               // 查询头部（SKU/仓库/期间筛选）
    ├── summary-tab.vue          // 汇总账标签页
    │   └── summary-table.vue    // 汇总表格
    ├── detail-tab.vue           // 明细账标签页
    │   └── detail-table.vue     // 明细表格
    ├── check-tab.vue            // 勾稽校验标签页
    │   └── check-table.vue      // 校验结果表格
    ├── chart.vue                // 库存趋势图
    ├── voucher-preview.vue      // 凭证预览组件
    └── export-dialog.vue        // 导出弹窗
```

**移动加权平均计算规则**:

```
入库时：
新单价 = (原库存金额 + 本次入库金额) / (原库存数量 + 本次入库数量)

出库时：
出库金额 = 出库数量 × 当前移动平均单价

示例：
期初：100件，单价10元，金额1000元
入库：50件，单价12元，金额600元
新单价 = (1000 + 600) / (100 + 50) = 10.67元
出库：30件
出库金额 = 30 × 10.67 = 320元
结存：120件，金额1280元
```

**核心功能**:

1. **汇总账查询**
   - 按SKU+仓库维度汇总
   - 显示当前库存数量、金额、单价
   - 显示本期/累计变动

2. **明细账查询**
   - 按时间顺序展示所有变动
   - 支持按单据类型筛选
   - 展示移动加权平均单价计算过程

3. **勾稽校验**
   - 校验库存表与记录表数据一致性
   - 标记差异项
   - 提供修复建议

4. **凭证生成**
   - 采购入库凭证：
     ```
     借：库存商品_XX仓库（存货辅助核算）
     贷：在途库存_XX仓库
     ```
   - 销售出库凭证：
     ```
     借：主营业务成本
     贷：库存商品_XX仓库
     ```

5. **趋势图表**
   - 库存数量趋势图
   - 库存金额趋势图
   - 移动平均单价趋势图

**新增API**: `api/finance/inventoryLedger.js`

```javascript
// 接口清单
GET /api/finance/inventory/ledger/summary   // 汇总账
GET /api/finance/inventory/ledger/detail    // 明细账
GET /api/finance/inventory/ledger/check     // 勾稽校验
GET /api/finance/inventory/ledger/chart     // 趋势图数据
GET /api/finance/inventory/ledger/export    // 导出
POST /api/finance/inventory/ledger/voucher  // 生成凭证
```

---

## 四、路由配置修改

### 4.1 新增路由

```javascript
// router/modules/finance.js 新增路由

{
  path: '/fin/inventory-global',
  component: () => import('@/views/finance/config/inventory-global/index.vue'),
  name: 'InventoryGlobalConfig',
  meta: { title: '进销存全局配置', icon: 'setting' }
},
{
  path: '/fin/purchase-ledger',
  component: () => import('@/views/finance/purchase-ledger/index.vue'),
  name: 'PurchaseLedger',
  meta: { title: '采购账户台账', icon: 'ledger' }
},
{
  path: '/fin/invoice-ledger',
  component: () => import('@/views/finance/invoice-ledger/index.vue'),
  name: 'InvoiceLedger',
  meta: { title: '发票台账', icon: 'invoice' }
},
{
  path: '/fin/supplier-ledger',
  component: () => import('@/views/finance/supplier-ledger/index.vue'),
  name: 'SupplierLedger',
  meta: { title: '供应商台账', icon: 'supplier' }
},
{
  path: '/fin/inventory-ledger',
  component: () => import('@/views/finance/inventory-ledger/index.vue'),
  name: 'InventoryLedger',
  meta: { title: '库存台账', icon: 'inventory' }
}
```

**说明**: 采购账户管理已有路由 `erp/finance/account`，无需新增。

---

## 五、开发优先级与依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                      开发依赖关系（自上而下）                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────┐                                    │
│  │   基础配置模块       │  ◄── 第1周：无依赖，可优先开发      │
│  │  - 采购账户配置      │                                    │
│  │  - 供应商收款账户    │                                    │
│  │  - 进销存全局配置    │                                    │
│  └──────────┬──────────┘                                    │
│             │                                               │
│             ▼                                               │
│  ┌─────────────────────┐                                    │
│  │   采购订单（会计版）  │  ◄── 第2周：依赖基础配置           │
│  │  - 订单创建/查询     │                                    │
│  │  - 付款/退款/入库    │                                    │
│  └──────────┬──────────┘                                    │
│             │                                               │
│             ▼                                               │
│  ┌─────────────────────┐  ◄── 第3周：依赖采购订单数据       │
│  │   采购账户台账       │                                    │
│  │  - 台账列表查询      │                                    │
│  │  - 台账付款          │                                    │
│  │  - 批量对账单上传    │                                    │
│  └──────────┬──────────┘                                    │
│             │                                               │
│             ▼                                               │
│  ┌─────────────────────┐  ◄── 第4周：依赖采购订单           │
│  │   发票台账           │                                    │
│  │  - 发票同步          │                                    │
│  │  - 发票入账          │                                    │
│  │  - 供应商台账查询    │                                    │
│  └──────────┬──────────┘                                    │
│             │                                               │
│             ▼                                               │
│  ┌─────────────────────┐  ◄── 第5周：依赖库存变动数据       │
│  │   库存台账           │                                    │
│  │  - 移动加权平均计算  │                                    │
│  │  - 汇总账/明细账     │                                    │
│  │  - 勾稽校验          │                                    │
│  └─────────────────────┘                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、现有页面改造清单

| 序号 | 页面路径 | 改造内容 | 工作量 |
|-----|---------|---------|--------|
| 1 | `erp/finance/account/index.vue` | 增加会计科目关联、账户类型、配置JSON | 中 |
| 2 | `erp/purchase/orders/components/create.vue` | 增加采购账户选择、费用类型选择 | 中 |
| 3 | `erp/purchase/orders/components/payment.vue` | 增加会计版付款逻辑、凭证预览 | 中 |
| 4 | `erp/purchase/orders/components/receipt.vue` | 增加入库凭证生成逻辑 | 中 |
| 5 | `erp/purchase/orders/index.vue` | 增加四维状态展示 | 小 |
| 6 | `erp/baseinfo/supplier/index.vue` | 增加收款账户管理入口 | 小 |

---

## 七、新增API文件清单

| 序号 | 文件路径 | 功能 |
|-----|---------|------|
| 1 | `api/finance/purchaseAccount.js` | 采购账户管理 |
| 2 | `api/erp/supplierBank.js` | 供应商收款账户 |
| 3 | `api/finance/inventoryConfig.js` | 进销存全局配置 |
| 4 | `api/finance/purchaseOrder.js` | 采购订单（会计版） |
| 5 | `api/finance/purchaseLedger.js` | 采购账户台账 |
| 6 | `api/finance/invoiceLedger.js` | 发票台账 |
| 7 | `api/finance/supplierLedger.js` | 供应商台账 |
| 8 | `api/finance/inventoryLedger.js` | 库存台账 |

---

## 八、凭证生成规则（前端展示）

### 8.1 现金类账户付款

```
付款凭证：
  借：预付账款-ERP采购供应商（供应商辅助核算）
  贷：对应现金科目（如银行存款-浦发）

在途库存凭证（同时生成）：
  借：在途库存_XX本地仓库（存货辅助核算）
  贷：预付账款-ERP在途发票
```

### 8.2 账期类账户付款

```
付款凭证：
  借：预付账款-ERP采购供应商（供应商辅助核算）
  贷：应付账款_XXX（账期账户对应科目）

在途库存凭证（同时生成）：
  借：在途库存_XX本地仓库（存货辅助核算）
  贷：预付账款-ERP在途发票
```

### 8.3 入库

```
借：库存商品_XX本地仓库（存货辅助核算）
贷：在途库存_XX本地仓库
```

### 8.4 发票入账

```
借：预付账款-ERP在途发票
贷：预付账款-ERP采购供应商（供应商辅助核算）
```

---

## 九、验收标准

### 9.1 基础配置模块
- [x] 采购账户可正常增删改查（已有功能）
- [ ] 采购账户关联会计科目
- [ ] 采购账户类型区分（现金/账期）
- [ ] 配置JSON正确序列化和反序列化
- [ ] 供应商收款账户支持多账户，默认账户唯一
- [ ] 进销存全局配置保存成功

### 9.2 采购订单（会计版）
- [ ] 订单四维状态展示正确
- [ ] 创建订单时选择采购账户和费用类型
- [ ] 付款生成凭证，科目映射正确
- [ ] 入库更新库存，生成凭证
- [ ] 退款生成红字凭证

### 9.3 采购账户台账
- [ ] 台账列表查询正确
- [ ] 台账付款操作成功
- [ ] 批量上传对账单功能正常

### 9.4 发票台账与供应商台账
- [ ] 发票同步功能正常
- [ ] 发票入账生成凭证
- [ ] 供应商汇总查询正确

### 9.5 库存台账
- [ ] 移动加权平均计算正确
- [ ] 汇总账/明细账查询正确
- [ ] 勾稽校验逻辑正确

---

## 十、风险与注意事项

1. **数据一致性**: 前端展示的金额计算需要与后端保持一致，特别是移动加权平均单价
2. **状态同步**: 订单的四维状态需要实时同步更新
3. **凭证预览**: 付款/入库前需要提供凭证预览功能，让用户确认
4. **权限控制**: 不同角色的用户可能需要不同的操作权限
5. **性能优化**: 台账列表可能数据量较大，需要考虑分页和虚拟滚动

---

*文档生成时间: 2026-07-10*
*基于文档版本: v1.0 (2026-06-25)*
*台账设计补充: v1.1 (2026-07-10)*
