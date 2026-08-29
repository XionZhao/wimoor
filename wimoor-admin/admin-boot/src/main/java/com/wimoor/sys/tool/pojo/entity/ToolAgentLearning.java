package com.wimoor.sys.tool.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Agent自我学习记录表
 * 存储Agent从交互中学到的知识，与静态配置(t_sys_tool_agent_context)独立
 */
@TableName(value = "t_sys_agent_learning")
@Data
public class ToolAgentLearning {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 学习类型：
     * tool_routing - 工具选择纠正
     * param_correction - 参数使用纠正
     * multi_step_flow - 多步流程学习
     * business_knowledge - 业务知识积累
     * user_preference - 用户偏好
     * api_behavior_patch - 接口行为修正
     * conversation_pattern - 对话模式识别
     */
    private String learnType;

    /**
     * 学习记录唯一键，如 tool_routing:erp_material_query
     */
    private String learnKey;

    /**
     * 学习内容(JSON格式)，结构因learn_type不同而不同
     */
    private String content;

    /**
     * 置信度：0.00-1.00，初始0.5，命中/验证后提升
     */
    private BigDecimal confidence;

    /**
     * 状态：pending(待验证)/active(已生效)/rejected(已拒绝)/expired(已过期)
     */
    private String status;

    /**
     * 来源：user_correction(用户纠正)/agent_discovery(自动发现)/admin_input(管理员输入)
     */
    private String sourceType;

    /**
     * 来源会话ID
     */
    private String sourceSessionId;

    /**
     * 来源消息ID列表(JSON数组)
     */
    private String sourceMessageIds;

    /**
     * 生效范围：global(全局)/tenant(租户)/user(用户)
     */
    private String effectiveScope;

    /**
     * 作用域ID：tenant→shopid, user→userid
     */
    private Long scopeId;

    /**
     * 适用的工具名列表(JSON数组)，用于过滤注入
     */
    private String applicableTools;

    /**
     * 触发关键词/意图模式列表(JSON数组)
     */
    private String triggerPatterns;

    /**
     * 命中次数（被注入且用户未纠正）
     */
    private Integer hitCount;

    /**
     * 最近命中时间
     */
    private Date lastHitTime;

    /**
     * 是否经过人工验证：0未验证/1已验证
     */
    private Integer verified;

    /**
     * 验证人
     */
    private Long verifiedBy;

    /**
     * 验证时间
     */
    private Date verifiedTime;

    /**
     * 过期时间(NULL表示永不过期)
     */
    private Date expiresAt;

    /**
     * 创建人
     */
    private Long operator;

    /**
     * 备注
     */
    private String remark;

    private Date createtime;
    private Date opttime;

    // ========== 学习类型常量 ==========
    public static final String TYPE_TOOL_ROUTING = "tool_routing";
    public static final String TYPE_PARAM_CORRECTION = "param_correction";
    public static final String TYPE_MULTI_STEP_FLOW = "multi_step_flow";
    public static final String TYPE_BUSINESS_KNOWLEDGE = "business_knowledge";
    public static final String TYPE_USER_PREFERENCE = "user_preference";
    public static final String TYPE_API_BEHAVIOR_PATCH = "api_behavior_patch";
    public static final String TYPE_CONVERSATION_PATTERN = "conversation_pattern";

    // ========== 状态常量 ==========
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_EXPIRED = "expired";

    // ========== 来源常量 ==========
    public static final String SOURCE_USER_CORRECTION = "user_correction";
    public static final String SOURCE_AGENT_DISCOVERY = "agent_discovery";
    public static final String SOURCE_ADMIN_INPUT = "admin_input";

    // ========== 作用域常量 ==========
    public static final String SCOPE_GLOBAL = "global";
    public static final String SCOPE_TENANT = "tenant";
    public static final String SCOPE_USER = "user";
}
