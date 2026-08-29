package com.wimoor.sys.tool.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.sys.tool.pojo.entity.ToolAgentLearning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Agent自我学习记录Mapper
 */
@Mapper
public interface ToolAgentLearningMapper extends BaseMapper<ToolAgentLearning> {

    /**
     * 增加命中次数并更新最近命中时间
     */
    @Update("UPDATE t_sys_agent_learning SET hit_count = hit_count + 1, last_hit_time = NOW() WHERE id = #{id}")
    int incrementHitCount(@Param("id") Long id);

    /**
     * 提升置信度（每次命中+0.05，上限1.00）
     */
    @Update("UPDATE t_sys_agent_learning SET confidence = LEAST(1.00, confidence + 0.05) WHERE id = #{id}")
    int increaseConfidence(@Param("id") Long id);

    /**
     * 降低置信度（负面反馈-0.20）
     */
    @Update("UPDATE t_sys_agent_learning SET confidence = GREATEST(0.00, confidence - 0.20) WHERE id = #{id}")
    int decreaseConfidence(@Param("id") Long id);

    /**
     * 自动过期：将低置信度或长期未命中的记录标记为expired
     */
    @Update("UPDATE t_sys_agent_learning SET status = 'expired' WHERE status = 'active' AND (confidence < 0.20 OR (last_hit_time IS NOT NULL AND last_hit_time < DATE_SUB(NOW(), INTERVAL 90 DAY)))")
    int autoExpireStaleRecords();

    /**
     * 自动激活：高置信度且多次命中的pending记录自动转为active
     */
    @Update("UPDATE t_sys_agent_learning SET status = 'active' WHERE status = 'pending' AND confidence >= 0.80 AND hit_count >= 5")
    int autoActivateHighConfidenceRecords();
}
