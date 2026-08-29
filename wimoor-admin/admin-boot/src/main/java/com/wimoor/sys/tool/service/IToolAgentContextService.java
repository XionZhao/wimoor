package com.wimoor.sys.tool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.sys.tool.pojo.entity.ToolAgentContext;

import java.util.List;

/**
 * Agent上下文配置Service
 */
public interface IToolAgentContextService extends IService<ToolAgentContext> {

    /**
     * 获取所有启用的上下文配置
     */
    List<ToolAgentContext> getAllEnabled();

    /**
     * 按类型获取启用的上下文配置
     */
    List<ToolAgentContext> getByType(String contextType);

    /**
     * 加载并生成上下文描述文本（用于系统提示词）
     * 如果数据库无数据，返回null，调用方应fallback到JSON文件
     */
    String loadContextDescription();
}
