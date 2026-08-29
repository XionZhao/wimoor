package com.wimoor.feishu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.feishu.pojo.entity.FeishuChatGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 飞书群组Mapper
 */
@Mapper
public interface FeishuChatGroupMapper extends BaseMapper<FeishuChatGroup> {

    /**
     * 查询群组列表（含消息数量统计）
     */
    List<Map<String, Object>> getGroupListWithStats(@Param("appId") String appId,@Param("username") String username);
}
