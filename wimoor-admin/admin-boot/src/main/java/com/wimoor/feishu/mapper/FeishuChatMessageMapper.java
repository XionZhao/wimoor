package com.wimoor.feishu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.feishu.pojo.entity.FeishuChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 飞书聊天消息Mapper
 */
@Mapper
public interface FeishuChatMessageMapper extends BaseMapper<FeishuChatMessage> {
}
