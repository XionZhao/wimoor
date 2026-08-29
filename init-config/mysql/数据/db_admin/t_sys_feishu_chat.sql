-- =============================================
-- 飞书群聊记录相关表
-- 前缀: t_sys_feishu_chat_
-- 按app_id区分，不存储shopid
-- =============================================

-- 1. 群组表
DROP TABLE IF EXISTS `t_sys_feishu_chat_group`;
CREATE TABLE `t_sys_feishu_chat_group` (
  `id` VARCHAR(64) NOT NULL COMMENT '群组ID（飞书chat_id）',
  `name` VARCHAR(255) DEFAULT NULL COMMENT '群名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '群描述',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '群头像URL',
  `owner_id` VARCHAR(64) DEFAULT NULL COMMENT '群主open_id',
  `chat_mode` VARCHAR(32) DEFAULT NULL COMMENT '群模式: group/p2p',
  `member_count` INT DEFAULT 0 COMMENT '成员数量',
  `app_id` VARCHAR(64) DEFAULT NULL COMMENT '飞书应用ID',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 1正常 0已解散',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书群组表';

-- 2. 群成员/发言人表
DROP TABLE IF EXISTS `t_sys_feishu_chat_member`;
CREATE TABLE `t_sys_feishu_chat_member` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键（chat_id_open_id）',
  `chat_id` VARCHAR(64) NOT NULL COMMENT '群组ID',
  `name` VARCHAR(128) DEFAULT NULL COMMENT '成员名称',
  `en_name` VARCHAR(128) DEFAULT NULL COMMENT '英文名',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `member_id_type` VARCHAR(32) DEFAULT NULL COMMENT 'ID类型: open_id/user_id/union_id',
  `member_id` VARCHAR(64) DEFAULT NULL COMMENT '成员open_id',
  `tenant_key` VARCHAR(64) DEFAULT NULL COMMENT '租户key',
  `is_bot` TINYINT DEFAULT 0 COMMENT '是否机器人: 0否 1是',
  `first_message_time` DATETIME DEFAULT NULL COMMENT '首次发言时间',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后发言时间',
  `message_count` INT DEFAULT 0 COMMENT '消息数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_chat_id` (`chat_id`),
  UNIQUE KEY `uk_chat_member` (`chat_id`, `member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书群成员表';

-- 3. 聊天消息表
DROP TABLE IF EXISTS `t_sys_feishu_chat_message`;
CREATE TABLE `t_sys_feishu_chat_message` (
  `id` VARCHAR(128) NOT NULL COMMENT '消息ID（飞书message_id）',
  `chat_id` VARCHAR(64) NOT NULL COMMENT '群组ID',
  `sender_id` VARCHAR(64) DEFAULT NULL COMMENT '发送者ID（open_id）',
  `sender_name` VARCHAR(128) DEFAULT NULL COMMENT '发送者名称',
  `msg_type` VARCHAR(32) DEFAULT NULL COMMENT '消息类型: text/post/image/file/audio/media/sticker/share_chat/share_user/interactive',
  `content` TEXT DEFAULT NULL COMMENT '消息内容（JSON格式）',
  `content_text` TEXT DEFAULT NULL COMMENT '纯文本内容（便于搜索）',
  `parent_id` VARCHAR(128) DEFAULT NULL COMMENT '父消息ID（回复消息时有值）',
  `root_id` VARCHAR(128) DEFAULT NULL COMMENT '根消息ID（话题消息时有值）',
  `chat_type` VARCHAR(16) DEFAULT NULL COMMENT '会话类型: p2p/group',
  `mention_users` VARCHAR(500) DEFAULT NULL COMMENT '被@的用户列表(JSON数组)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
  `insert_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `app_id` VARCHAR(64) DEFAULT NULL COMMENT '飞书应用ID',
  PRIMARY KEY (`id`),
  KEY `idx_chat_id` (`chat_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书聊天消息表';

-- 4. 聊天文件表
DROP TABLE IF EXISTS `t_sys_feishu_chat_file`;
CREATE TABLE `t_sys_feishu_chat_file` (
  `id` VARCHAR(128) NOT NULL COMMENT '主键（file_key或image_key）',
  `message_id` VARCHAR(128) NOT NULL COMMENT '关联消息ID',
  `chat_id` VARCHAR(64) NOT NULL COMMENT '群组ID',
  `sender_id` VARCHAR(64) DEFAULT NULL COMMENT '发送者ID',
  `file_type` VARCHAR(32) DEFAULT NULL COMMENT '文件类型: image/file/audio/media',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `file_key` VARCHAR(128) DEFAULT NULL COMMENT '飞书文件key',
  `image_key` VARCHAR(128) DEFAULT NULL COMMENT '飞书图片key',
  `local_path` VARCHAR(500) DEFAULT NULL COMMENT '本地存储路径',
  `duration` INT DEFAULT 0 COMMENT '音视频时长（秒）',
  `download_status` TINYINT DEFAULT 0 COMMENT '下载状态: 0未下载 1已下载 2下载失败',
  `app_id` VARCHAR(64) DEFAULT NULL COMMENT '飞书应用ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_chat_id` (`chat_id`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='飞书聊天文件表';
