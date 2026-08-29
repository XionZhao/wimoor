package com.wimoor.sys.tool.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.sys.tool.mapper.ToolAgentLearningFeedbackMapper;
import com.wimoor.sys.tool.mapper.ToolAgentLearningMapper;
import com.wimoor.sys.tool.pojo.entity.ToolAgentLearning;
import com.wimoor.sys.tool.pojo.entity.ToolAgentLearningFeedback;
import com.wimoor.sys.tool.service.IToolAgentLearningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Agent自我学习Service实现
 */
@Slf4j
@Service
public class ToolAgentLearningServiceImpl extends ServiceImpl<ToolAgentLearningMapper, ToolAgentLearning>
        implements IToolAgentLearningService {

    @Resource
    private ToolAgentLearningFeedbackMapper feedbackMapper;

    @Override
    public List<ToolAgentLearning> getActiveLearnings(Long shopid, Long userid) {
        return list(new LambdaQueryWrapper<ToolAgentLearning>()
                .eq(ToolAgentLearning::getStatus, ToolAgentLearning.STATUS_ACTIVE)
                .ge(ToolAgentLearning::getConfidence, new BigDecimal("0.50"))
                .and(w -> w
                        .isNull(ToolAgentLearning::getExpiresAt)
                        .or()
                        .gt(ToolAgentLearning::getExpiresAt, new Date())
                )
                .and(w -> w
                        .eq(ToolAgentLearning::getEffectiveScope, ToolAgentLearning.SCOPE_GLOBAL)
                        .or(inner -> inner
                                .eq(ToolAgentLearning::getEffectiveScope, ToolAgentLearning.SCOPE_TENANT)
                                .eq(ToolAgentLearning::getScopeId, shopid)
                        )
                        .or(inner -> inner
                                .eq(ToolAgentLearning::getEffectiveScope, ToolAgentLearning.SCOPE_USER)
                                .eq(ToolAgentLearning::getScopeId, userid)
                        )
                )
                .orderByDesc(ToolAgentLearning::getConfidence)
                .orderByDesc(ToolAgentLearning::getHitCount)
        );
    }

    @Override
    public boolean recordLearning(ToolAgentLearning learning) {
        // 检查learn_key是否已存在
        ToolAgentLearning existing = getOne(new LambdaQueryWrapper<ToolAgentLearning>()
                .eq(ToolAgentLearning::getLearnKey, learning.getLearnKey()));
        if (existing != null) {
            // 已存在则更新content和触发模式，但保留原有的置信度和命中次数
            existing.setContent(learning.getContent());
            if (learning.getTriggerPatterns() != null) {
                existing.setTriggerPatterns(learning.getTriggerPatterns());
            }
            existing.setOpttime(new Date());
            return updateById(existing);
        }
        // 新记录设置默认值
        if (learning.getConfidence() == null) {
            learning.setConfidence(new BigDecimal("0.50"));
        }
        if (learning.getStatus() == null) {
            learning.setStatus(ToolAgentLearning.STATUS_PENDING);
        }
        if (learning.getHitCount() == null) {
            learning.setHitCount(0);
        }
        if (learning.getVerified() == null) {
            learning.setVerified(0);
        }
        if (learning.getEffectiveScope() == null) {
            learning.setEffectiveScope(ToolAgentLearning.SCOPE_GLOBAL);
        }
        return save(learning);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFeedback(Long learningId, String sessionId, Long userId, String feedbackType, String feedbackDetail) {
        // 1. 保存反馈记录
        ToolAgentLearningFeedback feedback = new ToolAgentLearningFeedback();
        feedback.setLearningId(learningId);
        feedback.setSessionId(sessionId);
        feedback.setUserId(userId);
        feedback.setFeedbackType(feedbackType);
        feedback.setFeedbackDetail(feedbackDetail);
        feedbackMapper.insert(feedback);

        // 2. 根据反馈类型调整置信度
        ToolAgentLearning learning = getById(learningId);
        if (learning == null) return;

        switch (feedbackType) {
            case ToolAgentLearningFeedback.FEEDBACK_POSITIVE:
                // 正面反馈：置信度+0.10
                learning.setConfidence(learning.getConfidence().add(new BigDecimal("0.10")).min(new BigDecimal("1.00")));
                break;
            case ToolAgentLearningFeedback.FEEDBACK_NEGATIVE:
                // 负面反馈：置信度-0.20
                learning.setConfidence(learning.getConfidence().subtract(new BigDecimal("0.20")).max(new BigDecimal("0.00")));
                break;
            default:
                break;
        }

        // 3. 自动状态转换
        if (learning.getConfidence().compareTo(new BigDecimal("0.20")) < 0) {
            learning.setStatus(ToolAgentLearning.STATUS_EXPIRED);
        }

        updateById(learning);
    }

    @Override
    public void recordHit(Long learningId) {
        baseMapper.incrementHitCount(learningId);
        baseMapper.increaseConfidence(learningId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void performMaintenance() {
        // 自动过期低质量记录
        int expired = baseMapper.autoExpireStaleRecords();
        if (expired > 0) {
            log.info("Agent学习维护：自动过期 {} 条低质量记录", expired);
        }
        // 自动激活高置信度记录
        int activated = baseMapper.autoActivateHighConfidenceRecords();
        if (activated > 0) {
            log.info("Agent学习维护：自动激活 {} 条高置信度记录", activated);
        }
    }

    @Override
    public String buildLearningPrompt(Long shopid, Long userid, int maxChars) {
        List<ToolAgentLearning> learnings = getActiveLearnings(shopid, userid);
        if (learnings == null || learnings.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n## 从历史交互中学到的知识（请优先参考）\n");

        // 按类型分组展示
        String currentType = "";
        for (ToolAgentLearning learning : learnings) {
            JSONObject content = JSONObject.parseObject(learning.getContent());
            if (content == null) continue;

            // 按类型分组标题
            String typeHeader = getTypeHeader(learning.getLearnType());
            if (!learning.getLearnType().equals(currentType)) {
                sb.append("\n### ").append(typeHeader).append("\n");
                currentType = learning.getLearnType();
            }

            // 生成学习条目
            String entry = formatLearningEntry(learning, content);
            if (sb.length() + entry.length() > maxChars) {
                sb.append("（更多学习记录已省略）\n");
                break;
            }
            sb.append(entry);
        }

        return sb.toString();
    }

    /**
     * 获取学习类型的中文标题
     */
    private String getTypeHeader(String learnType) {
        switch (learnType) {
            case ToolAgentLearning.TYPE_TOOL_ROUTING: return "工具选择规则";
            case ToolAgentLearning.TYPE_PARAM_CORRECTION: return "参数使用注意事项";
            case ToolAgentLearning.TYPE_MULTI_STEP_FLOW: return "查询流程";
            case ToolAgentLearning.TYPE_BUSINESS_KNOWLEDGE: return "业务知识";
            case ToolAgentLearning.TYPE_USER_PREFERENCE: return "用户偏好";
            case ToolAgentLearning.TYPE_API_BEHAVIOR_PATCH: return "接口补充说明";
            case ToolAgentLearning.TYPE_CONVERSATION_PATTERN: return "常用查询模式";
            default: return learnType;
        }
    }

    /**
     * 格式化单条学习记录为提示词文本
     */
    private String formatLearningEntry(ToolAgentLearning learning, JSONObject content) {
        StringBuilder sb = new StringBuilder("- ");
        switch (learning.getLearnType()) {
            case ToolAgentLearning.TYPE_TOOL_ROUTING:
                sb.append("当用户查询\"").append(content.getString("intent_pattern")).append("\"时，");
                sb.append("应使用 ").append(content.getString("correct_tool"));
                String wrongTool = content.getString("wrong_tool");
                if (wrongTool != null) {
                    sb.append("，不要用 ").append(wrongTool);
                }
                break;
            case ToolAgentLearning.TYPE_PARAM_CORRECTION:
                sb.append(content.getString("tool_name")).append(" 的参数 \"");
                sb.append(content.getString("correct_param")).append("\" 是正确的");
                String wrongParam = content.getString("wrong_param");
                if (wrongParam != null) {
                    sb.append("，不是 \"").append(wrongParam).append("\"");
                }
                break;
            case ToolAgentLearning.TYPE_MULTI_STEP_FLOW:
                sb.append(content.getString("flow_name")).append("：");
                sb.append(content.getString("description"));
                break;
            case ToolAgentLearning.TYPE_BUSINESS_KNOWLEDGE:
                sb.append("[").append(content.getString("entity_type")).append(":");
                sb.append(content.getString("entity_key")).append("] ");
                sb.append(content.getString("content"));
                break;
            case ToolAgentLearning.TYPE_USER_PREFERENCE:
                sb.append(content.getString("description"));
                break;
            case ToolAgentLearning.TYPE_API_BEHAVIOR_PATCH:
                sb.append(content.getString("tool_name")).append("：");
                sb.append(content.getString("description"));
                break;
            case ToolAgentLearning.TYPE_CONVERSATION_PATTERN:
                sb.append(content.getString("pattern_name")).append("：");
                sb.append(content.getString("description"));
                break;
            default:
                sb.append(content.toJSONString());
        }
        sb.append(" [置信度:").append(learning.getConfidence());
        if (learning.getHitCount() > 0) {
            sb.append(", 命中").append(learning.getHitCount()).append("次");
        }
        sb.append("]\n");
        return sb.toString();
    }
}
