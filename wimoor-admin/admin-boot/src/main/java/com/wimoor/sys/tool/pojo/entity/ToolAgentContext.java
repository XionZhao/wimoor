package com.wimoor.sys.tool.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Agent上下文配置表
 * 存储业务域规则、实体关联关系、常见流程等，支持动态管理
 */
@TableName(value = "t_sys_tool_agent_context")
@Data
public class ToolAgentContext {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置类型：business_domain/entity_relation/field_alias/common_flow/page_hint
     */
    private String contextType;

    /**
     * 配置键名，如 amazon_domain/erp_domain/material→purchase
     */
    private String contextKey;

    /**
     * 配置内容(JSON格式)
     */
    private String content;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否启用：0禁用/1启用
     */
    private Integer isEnabled;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 操作人
     */
    private Long operator;

    private Date createtime;
    private Date opttime;
}
