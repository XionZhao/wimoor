package com.wimoor.feishu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.feishu.pojo.entity.FeishuChatFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 飞书聊天文件Mapper
 */
@Mapper
public interface FeishuChatFileMapper extends BaseMapper<FeishuChatFile> {
}
