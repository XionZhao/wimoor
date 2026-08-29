package com.wimoor.sys.tool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.sys.tool.pojo.entity.ToolAgentLearning;

import java.util.List;

/**
 * Agent自我学习Service
 */
public interface IToolAgentLearningService extends IService<ToolAgentLearning> {

    /**
     * 获取适用于当前用户的学习记录（global + 当前租户 + 当前用户）
     * 只返回 status=active 且 confidence>=0.5 且未过期的记录
     *
     * @param shopid 租户ID
     * @param userid 用户ID
     * @return 适用于当前用户的学习记录列表
     */
    List<ToolAgentLearning> getActiveLearnings(Long shopid, Long userid);

    /**
     * 记录一次学习
     *
     * @param learning 学习记录
     * @return 是否成功（learn_key唯一约束，重复则忽略）
     */
    boolean recordLearning(ToolAgentLearning learning);

    /**
     * 记录反馈并调整置信度
     *
     * @param learningId 学习记录ID
     * @param sessionId  会话ID
     * @param userId     用户ID
     * @param feedbackType 反馈类型：positive/negative/neutral
     * @param feedbackDetail 反馈详情
     */
    void recordFeedback(Long learningId, String sessionId, Long userId, String feedbackType, String feedbackDetail);

    /**
     * 增加命中计数（被注入后用户未纠正视为命中）
     *
     * @param learningId 学习记录ID
     */
    void recordHit(Long learningId);

    /**
     * 执行定期维护：自动过期低质量记录，自动激活高置信度记录
     */
    void performMaintenance();

    /**
     * 生成学习知识的系统提示词文本
     * 只返回适用于当前用户的 active 记录，按置信度降序排列
     * 控制总长度不超过 maxChars
     *
     * @param shopid   租户ID
     * @param userid   用户ID
     * @param maxChars 最大字符数（默认2000）
     * @return 学习知识的提示词文本，无学习记录时返回空字符串
     */
    String buildLearningPrompt(Long shopid, Long userid, int maxChars);
}
