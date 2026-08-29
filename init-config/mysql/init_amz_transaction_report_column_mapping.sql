-- ============================================================
-- 交易报告 CSV 列名映射表初始化数据
-- ============================================================

-- 1. 创建表
CREATE TABLE IF NOT EXISTS `t_amz_transaction_report_column_mapping` (
    `id` BIGINT(20) UNSIGNED NOT NULL,
    `marketplaceid` VARCHAR(20) NOT NULL COMMENT '站点ID，default表示通用兜底',
    `field_name` VARCHAR(64) NOT NULL COMMENT 'Java实体字段名',
    `column_name` VARCHAR(255) NOT NULL COMMENT 'CSV列名(lowercase后)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_market_field_col` (`marketplaceid`, `field_name`, `column_name`),
    KEY `idx_marketplaceid` (`marketplaceid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易报告CSV列名映射表';

-- ============================================================
-- 2. default (通用兜底，US English 标准列名)
--    当指定 marketplaceid 查不到时，fallback 到 default
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
-- 文本字段
(1,  'default', 'dateTime',             'date/time'),
(2,  'default', 'settlementId',         'settlement id'),
(3,  'default', 'transactionType',      'type'),
(4,  'default', 'orderId',              'order id'),
(5,  'default', 'sku',                  'sku'),
(6,  'default', 'description',          'description'),
(7,  'default', 'quantity',             'quantity'),
(8,  'default', 'marketplace',          'marketplace'),
(9,  'default', 'fulfillment',          'account type'),
(10, 'default', 'fulfillment',          'fulfillment'),
(11, 'default', 'orderCity',            'order city'),
(12, 'default', 'orderState',           'order state'),
(13, 'default', 'orderPostCode',        'order postal'),
(14, 'default', 'taxCollectionModel',   'tax collection model'),
-- 数值字段
(15, 'default', 'productSales',             'product sales'),
(16, 'default', 'productSalesTax',          'product sales tax'),
(17, 'default', 'shippingCredits',          'shipping credits'),
(18, 'default', 'shippingCreditsTax',       'shipping credits tax'),
(19, 'default', 'giftwrapCredits',          'giftwrap credits'),
(20, 'default', 'giftwrapCreditsTax',       'giftwrap credits tax'),
(21, 'default', 'promotionalRebates',       'promotional rebates'),
(22, 'default', 'promotionalRebatesTax',    'promotional rebates tax'),
(23, 'default', 'marketplaceWithheldTax',   'marketplace withheld tax'),
(24, 'default', 'sellingFees',              'selling fees'),
(25, 'default', 'fbaFees',                  'fba fees'),
(26, 'default', 'otherTransactionFees',     'other transaction fees'),
(27, 'default', 'other',                    'other'),
(28, 'default', 'total',                    'total'),
(29, 'default', 'transactionStatus',       'transaction status'),
(30, 'default', 'transactonReleaseDate',   'transaction release date');

-- ============================================================
-- 3. UK (A1F83G8C2ARO7P) — 英式英语变体
--    差异: fulfilment(单l), postage credits(非shipping), gift wrap credits(带空格)
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(101, 'A1F83G8C2ARO7P', 'dateTime',             'date/time'),
(102, 'A1F83G8C2ARO7P', 'settlementId',         'settlement id'),
(103, 'A1F83G8C2ARO7P', 'transactionType',      'type'),
(104, 'A1F83G8C2ARO7P', 'orderId',              'order id'),
(105, 'A1F83G8C2ARO7P', 'sku',                  'sku'),
(106, 'A1F83G8C2ARO7P', 'description',          'description'),
(107, 'A1F83G8C2ARO7P', 'quantity',             'quantity'),
(108, 'A1F83G8C2ARO7P', 'marketplace',          'marketplace'),
(109, 'A1F83G8C2ARO7P', 'fulfillment',          'fulfilment'),
(110, 'A1F83G8C2ARO7P', 'orderCity',            'order city'),
(111, 'A1F83G8C2ARO7P', 'orderState',           'order state'),
(112, 'A1F83G8C2ARO7P', 'orderPostCode',        'order postal'),
(113, 'A1F83G8C2ARO7P', 'taxCollectionModel',   'tax collection model'),
(114, 'A1F83G8C2ARO7P', 'productSales',             'product sales'),
(115, 'A1F83G8C2ARO7P', 'productSalesTax',          'product sales tax'),
(116, 'A1F83G8C2ARO7P', 'shippingCredits',          'postage credits'),
(117, 'A1F83G8C2ARO7P', 'shippingCreditsTax',       'shipping credits tax'),
(118, 'A1F83G8C2ARO7P', 'giftwrapCredits',          'gift wrap credits'),
(119, 'A1F83G8C2ARO7P', 'giftwrapCreditsTax',       'giftwrap credits tax'),
(120, 'A1F83G8C2ARO7P', 'promotionalRebates',       'promotional rebates'),
(121, 'A1F83G8C2ARO7P', 'promotionalRebatesTax',    'promotional rebates tax'),
(122, 'A1F83G8C2ARO7P', 'marketplaceWithheldTax',   'marketplace withheld tax'),
(123, 'A1F83G8C2ARO7P', 'sellingFees',              'selling fees'),
(124, 'A1F83G8C2ARO7P', 'fbaFees',                  'fba fees'),
(125, 'A1F83G8C2ARO7P', 'otherTransactionFees',     'other transaction fees'),
(126, 'A1F83G8C2ARO7P', 'other',                    'other'),
(127, 'A1F83G8C2ARO7P', 'total',                    'total'),
(128, 'A1F83G8C2ARO7P', 'transactionStatus',       'transaction status'),
(129, 'A1F83G8C2ARO7P', 'transactonReleaseDate',   'transaction release date');

-- ============================================================
-- 4. ES (A1RKKUPIHCS9HS) — 西班牙语
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(201, 'A1RKKUPIHCS9HS', 'dateTime',             'fecha y hora'),
(202, 'A1RKKUPIHCS9HS', 'settlementId',         'identificador de pago'),
(203, 'A1RKKUPIHCS9HS', 'transactionType',      'tipo'),
(204, 'A1RKKUPIHCS9HS', 'orderId',              'número de pedido'),
(205, 'A1RKKUPIHCS9HS', 'sku',                  'sku'),
(206, 'A1RKKUPIHCS9HS', 'description',          'descripción'),
(207, 'A1RKKUPIHCS9HS', 'quantity',             'cantidad'),
(208, 'A1RKKUPIHCS9HS', 'marketplace',          'web de amazon'),
(209, 'A1RKKUPIHCS9HS', 'fulfillment',          'gestión logística'),
(210, 'A1RKKUPIHCS9HS', 'orderCity',            'ciudad de procedencia del pedido'),
(211, 'A1RKKUPIHCS9HS', 'orderState',           'comunidad autónoma de procedencia del pedido'),
(212, 'A1RKKUPIHCS9HS', 'orderPostCode',        'código postal de procedencia del pedido'),
(213, 'A1RKKUPIHCS9HS', 'taxCollectionModel',   'formulario de recaudación de impuestos'),
(214, 'A1RKKUPIHCS9HS', 'productSales',             'ventas de productos'),
(215, 'A1RKKUPIHCS9HS', 'productSalesTax',          'impuesto de ventas de productos'),
(216, 'A1RKKUPIHCS9HS', 'shippingCredits',          'abonos de envío'),
(217, 'A1RKKUPIHCS9HS', 'shippingCreditsTax',       'impuestos por abonos de envío'),
(218, 'A1RKKUPIHCS9HS', 'giftwrapCredits',          'abonos de envoltorio para regalo'),
(219, 'A1RKKUPIHCS9HS', 'giftwrapCreditsTax',       'impuestos por abonos de envoltorio para regalo'),
(220, 'A1RKKUPIHCS9HS', 'promotionalRebates',       'devoluciones promocionales'),
(221, 'A1RKKUPIHCS9HS', 'promotionalRebatesTax',    'impuestos de descuentos por promociones'),
(222, 'A1RKKUPIHCS9HS', 'marketplaceWithheldTax',   'impuesto retenido en el sitio web'),
(223, 'A1RKKUPIHCS9HS', 'sellingFees',              'tarifas de venta'),
(224, 'A1RKKUPIHCS9HS', 'fbaFees',                  'tarifas de logística de amazon'),
(225, 'A1RKKUPIHCS9HS', 'otherTransactionFees',     'tarifas de otras transacciones'),
(226, 'A1RKKUPIHCS9HS', 'other',                    'otro'),
(227, 'A1RKKUPIHCS9HS', 'total',                    'total');

-- ============================================================
-- 5. DE (A1PA6795UKMFR9) — 德语
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(301, 'A1PA6795UKMFR9', 'dateTime',             'datum/zeit'),
(302, 'A1PA6795UKMFR9', 'settlementId',         'abrechnungs-id'),
(303, 'A1PA6795UKMFR9', 'transactionType',      'typ'),
(304, 'A1PA6795UKMFR9', 'orderId',              'bestellnummer'),
(305, 'A1PA6795UKMFR9', 'sku',                  'sku'),
(306, 'A1PA6795UKMFR9', 'description',          'beschreibung'),
(307, 'A1PA6795UKMFR9', 'quantity',             'menge'),
(308, 'A1PA6795UKMFR9', 'marketplace',          'verkaufsplattform'),
(309, 'A1PA6795UKMFR9', 'fulfillment',          'versandart'),
(310, 'A1PA6795UKMFR9', 'orderCity',            'bestellstadt'),
(311, 'A1PA6795UKMFR9', 'orderState',           'bundesland'),
(312, 'A1PA6795UKMFR9', 'orderPostCode',        'postleitzahl'),
(313, 'A1PA6795UKMFR9', 'taxCollectionModel',   'steuererhebungsmodell'),
(314, 'A1PA6795UKMFR9', 'productSales',             'produktverkäufe'),
(315, 'A1PA6795UKMFR9', 'productSalesTax',          'produktumsatzsteuer'),
(316, 'A1PA6795UKMFR9', 'shippingCredits',          'versandgutschriften'),
(317, 'A1PA6795UKMFR9', 'shippingCreditsTax',       'versandgutschrift-steuer'),
(318, 'A1PA6795UKMFR9', 'giftwrapCredits',          'geschenkverpackungsgutschriften'),
(319, 'A1PA6795UKMFR9', 'giftwrapCreditsTax',       'geschenkverpackungsgutschrift-steuer'),
(320, 'A1PA6795UKMFR9', 'promotionalRebates',       'werbegutschriften'),
(321, 'A1PA6795UKMFR9', 'promotionalRebatesTax',    'werbegutschrift-steuer'),
(322, 'A1PA6795UKMFR9', 'marketplaceWithheldTax',   'einbehaltene marktplatzsteuer'),
(323, 'A1PA6795UKMFR9', 'sellingFees',              'verkaufsgebühren'),
(324, 'A1PA6795UKMFR9', 'fbaFees',                  'fba-gebühren'),
(325, 'A1PA6795UKMFR9', 'otherTransactionFees',     'andere transaktionsgebühren'),
(326, 'A1PA6795UKMFR9', 'other',                    'sonstiges'),
(327, 'A1PA6795UKMFR9', 'total',                    'gesamt');

-- ============================================================
-- 6. FR (A13V1IB3VIYZZH) — 法语
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(401, 'A13V1IB3VIYZZH', 'dateTime',             'date/heure'),
(402, 'A13V1IB3VIYZZH', 'settlementId',         'identifiant du paiement'),
(403, 'A13V1IB3VIYZZH', 'transactionType',      'type'),
(404, 'A13V1IB3VIYZZH', 'orderId',              'numéro de la commande'),
(405, 'A13V1IB3VIYZZH', 'sku',                  'sku'),
(406, 'A13V1IB3VIYZZH', 'description',          'description'),
(407, 'A13V1IB3VIYZZH', 'quantity',             'quantité'),
(408, 'A13V1IB3VIYZZH', 'marketplace',          'place de marché'),
(409, 'A13V1IB3VIYZZH', 'fulfillment',          'expédition'),
(410, 'A13V1IB3VIYZZH', 'orderCity',            'ville de la commande'),
(411, 'A13V1IB3VIYZZH', 'orderState',           'état de la commande'),
(412, 'A13V1IB3VIYZZH', 'orderPostCode',        'code postal de la commande'),
(413, 'A13V1IB3VIYZZH', 'taxCollectionModel',   'modèle de collecte des taxes'),
(414, 'A13V1IB3VIYZZH', 'productSales',             'ventes de produits'),
(415, 'A13V1IB3VIYZZH', 'productSalesTax',          'taxe sur les ventes de produits'),
(416, 'A13V1IB3VIYZZH', 'shippingCredits',          'crédits d''expédition'),
(417, 'A13V1IB3VIYZZH', 'shippingCreditsTax',       'taxe sur les crédits d''expédition'),
(418, 'A13V1IB3VIYZZH', 'giftwrapCredits',          'crédits d''emballage cadeau'),
(419, 'A13V1IB3VIYZZH', 'giftwrapCreditsTax',       'taxe sur les crédits d''emballage cadeau'),
(420, 'A13V1IB3VIYZZH', 'promotionalRebates',       'remises promotionnelles'),
(421, 'A13V1IB3VIYZZH', 'promotionalRebatesTax',    'taxe sur les remises promotionnelles'),
(422, 'A13V1IB3VIYZZH', 'marketplaceWithheldTax',   'taxe retenue par la place de marché'),
(423, 'A13V1IB3VIYZZH', 'sellingFees',              'frais de vente'),
(424, 'A13V1IB3VIYZZH', 'fbaFees',                  'frais fba'),
(425, 'A13V1IB3VIYZZH', 'otherTransactionFees',     'autres frais de transaction'),
(426, 'A13V1IB3VIYZZH', 'other',                    'autres'),
(427, 'A13V1IB3VIYZZH', 'total',                    'total');

-- ============================================================
-- 7. IT (APJ6JRA9NG5V4) — 意大利语
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(501, 'APJ6JRA9NG5V4', 'dateTime',             'data/ora'),
(502, 'APJ6JRA9NG5V4', 'settlementId',         'identificativo del pagamento'),
(503, 'APJ6JRA9NG5V4', 'transactionType',      'tipo'),
(504, 'APJ6JRA9NG5V4', 'orderId',              'id ordine'),
(505, 'APJ6JRA9NG5V4', 'sku',                  'sku'),
(506, 'APJ6JRA9NG5V4', 'description',          'descrizione'),
(507, 'APJ6JRA9NG5V4', 'quantity',             'quantità'),
(508, 'APJ6JRA9NG5V4', 'marketplace',          'marketplace'),
(509, 'APJ6JRA9NG5V4', 'fulfillment',          'logistica'),
(510, 'APJ6JRA9NG5V4', 'orderCity',            'città dell''ordine'),
(511, 'APJ6JRA9NG5V4', 'orderState',           'stato dell''ordine'),
(512, 'APJ6JRA9NG5V4', 'orderPostCode',        'codice postale dell''ordine'),
(513, 'APJ6JRA9NG5V4', 'taxCollectionModel',   'modello di riscossione delle imposte'),
(514, 'APJ6JRA9NG5V4', 'productSales',             'vendite di prodotti'),
(515, 'APJ6JRA9NG5V4', 'productSalesTax',          'imposta sulle vendite di prodotti'),
(516, 'APJ6JRA9NG5V4', 'shippingCredits',          'crediti di spedizione'),
(517, 'APJ6JRA9NG5V4', 'shippingCreditsTax',       'imposta sui crediti di spedizione'),
(518, 'APJ6JRA9NG5V4', 'giftwrapCredits',          'crediti per confezione regalo'),
(519, 'APJ6JRA9NG5V4', 'giftwrapCreditsTax',       'imposta sui crediti per confezione regalo'),
(520, 'APJ6JRA9NG5V4', 'promotionalRebates',       'sconti promozionali'),
(521, 'APJ6JRA9NG5V4', 'promotionalRebatesTax',    'imposta sugli sconti promozionali'),
(522, 'APJ6JRA9NG5V4', 'marketplaceWithheldTax',   'imposta trattenuta dal marketplace'),
(523, 'APJ6JRA9NG5V4', 'sellingFees',              'commissioni di vendita'),
(524, 'APJ6JRA9NG5V4', 'fbaFees',                  'commissioni fba'),
(525, 'APJ6JRA9NG5V4', 'otherTransactionFees',     'altre commissioni di transazione'),
(526, 'APJ6JRA9NG5V4', 'other',                    'altro'),
(527, 'APJ6JRA9NG5V4', 'total',                    'totale');

-- ============================================================
-- 8. JP (A1VC38T7YXB528) — 日语
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(601, 'A1VC38T7YXB528', 'dateTime',             '日付/時刻'),
(602, 'A1VC38T7YXB528', 'settlementId',         '決済id'),
(603, 'A1VC38T7YXB528', 'transactionType',      'タイプ'),
(604, 'A1VC38T7YXB528', 'orderId',              '注文id'),
(605, 'A1VC38T7YXB528', 'sku',                  'sku'),
(606, 'A1VC38T7YXB528', 'description',          '説明'),
(607, 'A1VC38T7YXB528', 'quantity',             '数量'),
(608, 'A1VC38T7YXB528', 'marketplace',          'マーケットプレイス'),
(609, 'A1VC38T7YXB528', 'fulfillment',          '出荷方法'),
(610, 'A1VC38T7YXB528', 'orderCity',            '市区町村'),
(611, 'A1VC38T7YXB528', 'orderState',           '都道府県'),
(612, 'A1VC38T7YXB528', 'orderPostCode',        '郵便番号'),
(613, 'A1VC38T7YXB528', 'taxCollectionModel',   '税金徴収モデル'),
(614, 'A1VC38T7YXB528', 'productSales',             '商品売上'),
(615, 'A1VC38T7YXB528', 'productSalesTax',          '商品売上税'),
(616, 'A1VC38T7YXB528', 'shippingCredits',          'クレジット送料'),
(617, 'A1VC38T7YXB528', 'shippingCreditsTax',       '送料クレジット税'),
(618, 'A1VC38T7YXB528', 'giftwrapCredits',          'ギフト包装クレジット'),
(619, 'A1VC38T7YXB528', 'giftwrapCreditsTax',       'ギフト包装クレジット税'),
(620, 'A1VC38T7YXB528', 'promotionalRebates',       'プロモーションリベート'),
(621, 'A1VC38T7YXB528', 'promotionalRebatesTax',    'プロモーションリベート税'),
(622, 'A1VC38T7YXB528', 'marketplaceWithheldTax',   'マーケットプレイス徴収税'),
(623, 'A1VC38T7YXB528', 'sellingFees',              '販売手数料'),
(624, 'A1VC38T7YXB528', 'fbaFees',                  'fba手数料'),
(625, 'A1VC38T7YXB528', 'otherTransactionFees',     'その他の取引手数料'),
(626, 'A1VC38T7YXB528', 'other',                    'その他'),
(627, 'A1VC38T7YXB528', 'total',                    '合計');

-- ============================================================
-- 9. NL (A1805IZSGTT6HS) — 荷兰语
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(701, 'A1805IZSGTT6HS', 'dateTime',             'datum/tijd'),
(702, 'A1805IZSGTT6HS', 'settlementId',         'identificatie van de afrekening'),
(703, 'A1805IZSGTT6HS', 'transactionType',      'type'),
(704, 'A1805IZSGTT6HS', 'orderId',              'bestelnummer'),
(705, 'A1805IZSGTT6HS', 'sku',                  'sku'),
(706, 'A1805IZSGTT6HS', 'description',          'omschrijving'),
(707, 'A1805IZSGTT6HS', 'quantity',             'hoeveelheid'),
(708, 'A1805IZSGTT6HS', 'marketplace',          'amazon-website'),
(709, 'A1805IZSGTT6HS', 'fulfillment',          'verzending'),
(710, 'A1805IZSGTT6HS', 'orderCity',            'woonplaats'),
(711, 'A1805IZSGTT6HS', 'orderState',           'provincie'),
(712, 'A1805IZSGTT6HS', 'orderPostCode',        'postcode'),
(713, 'A1805IZSGTT6HS', 'taxCollectionModel',   'model voor belastinginning'),
(714, 'A1805IZSGTT6HS', 'productSales',             'verkoop van producten'),
(715, 'A1805IZSGTT6HS', 'productSalesTax',          'belasting op productverkoop'),
(716, 'A1805IZSGTT6HS', 'shippingCredits',          'verzendkredieten'),
(717, 'A1805IZSGTT6HS', 'shippingCreditsTax',       'belasting op verzendkredieten'),
(718, 'A1805IZSGTT6HS', 'giftwrapCredits',          'cadeaubonnen voor inpakken'),
(719, 'A1805IZSGTT6HS', 'giftwrapCreditsTax',       'belasting op cadeaubonnen voor inpakken'),
(720, 'A1805IZSGTT6HS', 'promotionalRebates',       'promotiekortingen'),
(721, 'A1805IZSGTT6HS', 'promotionalRebatesTax',    'belasting op promotiekortingen'),
(722, 'A1805IZSGTT6HS', 'marketplaceWithheldTax',   'ingehouden belasting door marketplace'),
(723, 'A1805IZSGTT6HS', 'sellingFees',              'verkoopkosten'),
(724, 'A1805IZSGTT6HS', 'fbaFees',                  'fba-kosten'),
(725, 'A1805IZSGTT6HS', 'otherTransactionFees',     'andere transactiekosten'),
(726, 'A1805IZSGTT6HS', 'other',                    'overig'),
(727, 'A1805IZSGTT6HS', 'total',                    'totaal');

-- ============================================================
-- 10. IN (A21TJRUUN4KGV) — 印度英语 (有 GST/TCS/TDS 额外列)
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(801, 'A21TJRUUN4KGV', 'dateTime',             'date/time'),
(802, 'A21TJRUUN4KGV', 'settlementId',         'settlement id'),
(803, 'A21TJRUUN4KGV', 'transactionType',      'type'),
(804, 'A21TJRUUN4KGV', 'orderId',              'order id'),
(805, 'A21TJRUUN4KGV', 'sku',                  'sku'),
(806, 'A21TJRUUN4KGV', 'description',          'description'),
(807, 'A21TJRUUN4KGV', 'quantity',             'quantity'),
(808, 'A21TJRUUN4KGV', 'marketplace',          'marketplace'),
(809, 'A21TJRUUN4KGV', 'fulfillment',          'account type'),
(810, 'A21TJRUUN4KGV', 'orderCity',            'order city'),
(811, 'A21TJRUUN4KGV', 'orderState',           'order state'),
(812, 'A21TJRUUN4KGV', 'orderPostCode',        'order postal'),
(813, 'A21TJRUUN4KGV', 'productSales',             'product sales'),
(814, 'A21TJRUUN4KGV', 'productSalesTax',          'total sales tax liable(gst before adjusting tcs)'),
(815, 'A21TJRUUN4KGV', 'shippingCredits',          'shipping credits'),
(816, 'A21TJRUUN4KGV', 'giftwrapCredits',          'gift wrap credits'),
(817, 'A21TJRUUN4KGV', 'promotionalRebates',       'promotional rebates'),
(818, 'A21TJRUUN4KGV', 'sellingFees',              'selling fees'),
(819, 'A21TJRUUN4KGV', 'fbaFees',                  'fba fees'),
(820, 'A21TJRUUN4KGV', 'otherTransactionFees',     'other transaction fees'),
(821, 'A21TJRUUN4KGV', 'other',                    'other'),
(822, 'A21TJRUUN4KGV', 'total',                    'total');

-- ============================================================
-- 10. PL (A1C3SOZRARQ6R3) — 波兰语
--    注意: PL 无 taxCollectionModel, shippingCreditsTax, giftwrapCreditsTax, promotionalRebatesTax 列
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(901, 'A1C3SOZRARQ6R3', 'dateTime',             'data/godzina'),
(902, 'A1C3SOZRARQ6R3', 'settlementId',         'identyfikator rozliczenia'),
(903, 'A1C3SOZRARQ6R3', 'transactionType',      'typ'),
(904, 'A1C3SOZRARQ6R3', 'orderId',              'identyfikator zamówienia'),
(905, 'A1C3SOZRARQ6R3', 'sku',                  'sku'),
(906, 'A1C3SOZRARQ6R3', 'description',          'opis'),
(907, 'A1C3SOZRARQ6R3', 'quantity',             'ilość'),
(908, 'A1C3SOZRARQ6R3', 'marketplace',          'rynek'),
(909, 'A1C3SOZRARQ6R3', 'fulfillment',          'realizacja'),
(910, 'A1C3SOZRARQ6R3', 'orderCity',            'miejscowość zamówienia'),
(911, 'A1C3SOZRARQ6R3', 'orderState',           'stan zamówienia'),
(912, 'A1C3SOZRARQ6R3', 'orderPostCode',        'przekaz pocztowy'),
(913, 'A1C3SOZRARQ6R3', 'productSales',             'sprzedaż produktów'),
(914, 'A1C3SOZRARQ6R3', 'shippingCredits',          'noty kredytowe za wysyłkę'),
(915, 'A1C3SOZRARQ6R3', 'giftwrapCredits',          'środki na pokrycie pakowania na prezent'),
(916, 'A1C3SOZRARQ6R3', 'promotionalRebates',       'rabaty promocyjne'),
(917, 'A1C3SOZRARQ6R3', 'productSalesTax',          'pobrany podatek od sprzedaży'),
(918, 'A1C3SOZRARQ6R3', 'marketplaceWithheldTax',   'podatek od transakcji marketplace facilitator'),
(919, 'A1C3SOZRARQ6R3', 'sellingFees',              'opłaty za sprzedaż'),
(920, 'A1C3SOZRARQ6R3', 'fbaFees',                  'opłaty za fba'),
(921, 'A1C3SOZRARQ6R3', 'otherTransactionFees',     'inne opłaty transakcyjne'),
(922, 'A1C3SOZRARQ6R3', 'other',                    'inne'),
(923, 'A1C3SOZRARQ6R3', 'total',                    'suma'),
(924, 'A1C3SOZRARQ6R3', 'transactionStatus',       'status transakcji'),
(925, 'A1C3SOZRARQ6R3', 'transactonReleaseDate',   'data zrealizowania transakcji');

-- ============================================================
-- 11. SE (A2NODRKZP88ZB9) — 瑞典语
--    注意: SE 无 taxCollectionModel, shippingCreditsTax, giftwrapCreditsTax, promotionalRebatesTax 列
-- ============================================================
INSERT INTO t_amz_transaction_report_column_mapping (id, marketplaceid, field_name, column_name) VALUES
(1001, 'A2NODRKZP88ZB9', 'dateTime',             'datum/tid'),
(1002, 'A2NODRKZP88ZB9', 'settlementId',         'reglerings-id'),
(1003, 'A2NODRKZP88ZB9', 'transactionType',      'typ'),
(1004, 'A2NODRKZP88ZB9', 'orderId',              'beställnings-id'),
(1005, 'A2NODRKZP88ZB9', 'sku',                  'sku'),
(1006, 'A2NODRKZP88ZB9', 'description',          'beskrivning'),
(1007, 'A2NODRKZP88ZB9', 'quantity',             'antal'),
(1008, 'A2NODRKZP88ZB9', 'marketplace',          'marknadsplats'),
(1009, 'A2NODRKZP88ZB9', 'fulfillment',          'leverans'),
(1010, 'A2NODRKZP88ZB9', 'orderCity',            'stad för beställning'),
(1011, 'A2NODRKZP88ZB9', 'orderState',           'delstat för beställning'),
(1012, 'A2NODRKZP88ZB9', 'orderPostCode',        'postadress för beställning'),
(1013, 'A2NODRKZP88ZB9', 'productSales',             'försäljning av produkter'),
(1014, 'A2NODRKZP88ZB9', 'shippingCredits',          'fraktkrediter'),
(1015, 'A2NODRKZP88ZB9', 'giftwrapCredits',          'krediter för presentinslagning'),
(1016, 'A2NODRKZP88ZB9', 'promotionalRebates',       'kampanjrabatter'),
(1017, 'A2NODRKZP88ZB9', 'productSalesTax',          'inkasserad moms'),
(1018, 'A2NODRKZP88ZB9', 'marketplaceWithheldTax',   'skatt för marknadsplatsförmedlare'),
(1019, 'A2NODRKZP88ZB9', 'sellingFees',              'försäljningsavgifter'),
(1020, 'A2NODRKZP88ZB9', 'fbaFees',                  'fba-avgifter'),
(1021, 'A2NODRKZP88ZB9', 'otherTransactionFees',     'övriga transaktionsavgifter'),
(1022, 'A2NODRKZP88ZB9', 'other',                    'övrigt'),
(1023, 'A2NODRKZP88ZB9', 'total',                    'totalt'),
(1024, 'A2NODRKZP88ZB9', 'transactionStatus',       'transaktionsstatus'),
(1025, 'A2NODRKZP88ZB9', 'transactonReleaseDate',   'transaktionens utgivningsdatum');

-- ============================================================
-- 12. CA (A2EUQ1WTGCTBG2) — 同 US English
-- 13. AU (A39IBJ37TRP1C6) — 同 US English
-- 这两个站点查不到时 fallback 到 default，无需单独插入
-- ============================================================