package com.wimoor.feishu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.feishu.pojo.entity.FeishuChatMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * 飞书群成员Mapper
 */
@Mapper
public interface FeishuChatMemberMapper extends BaseMapper<FeishuChatMember> {
}
