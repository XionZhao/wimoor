package com.wimoor.sys.tool.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Agent学习反馈表
 * 记录学习记录的命中和反馈，用于置信度调整
 */
@TableName(value = "t_sys_agent_learning_feedback")
@Data
public class ToolAgentLearningFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的学习记录ID
     */
    private Long learningId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 反馈类型：positive(正确)/negative(错误)/neutral(无感)
     */
    private String feedbackType;

    /**
     * 反馈详情
     */
    private String feedbackDetail;

    private Date createtime;

    // ========== 反馈类型常量 ==========
    public static final String FEEDBACK_POSITIVE = "positive";
    public static final String FEEDBACK_NEGATIVE = "negative";
    public static final String FEEDBACK_NEUTRAL = "neutral";
}
