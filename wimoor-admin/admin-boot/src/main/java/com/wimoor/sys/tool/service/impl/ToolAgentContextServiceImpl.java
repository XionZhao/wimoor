package com.wimoor.sys.tool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.sys.tool.mapper.ToolAgentContextMapper;
import com.wimoor.sys.tool.pojo.entity.ToolAgentContext;
import com.wimoor.sys.tool.service.IToolAgentContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent上下文配置Service实现
 * 支持从数据库加载配置，如果数据库无数据则返回null让调用方fallback到JSON文件
 */
@Slf4j
@Service
public class ToolAgentContextServiceImpl extends ServiceImpl<ToolAgentContextMapper, ToolAgentContext>
        implements IToolAgentContextService {

    @Override
    public List<ToolAgentContext> getAllEnabled() {
        return list(new LambdaQueryWrapper<ToolAgentContext>()
                .eq(ToolAgentContext::getIsEnabled, 1)
                .orderByAsc(ToolAgentContext::getContextType, ToolAgentContext::getSortOrder));
    }

    @Override
    public List<ToolAgentContext> getByType(String contextType) {
        return list(new LambdaQueryWrapper<ToolAgentContext>()
                .eq(ToolAgentContext::getContextType, contextType)
                .eq(ToolAgentContext::getIsEnabled, 1)
                .orderByAsc(ToolAgentContext::getSortOrder));
    }

    @Override
    public String loadContextDescription() {
        List<ToolAgentContext> allConfigs = getAllEnabled();
        if (allConfigs == null || allConfigs.isEmpty()) {
            return null; // 数据库无数据，让调用方fallback到JSON文件
        }

        StringBuilder sb = new StringBuilder();

        // 按类型分组
        Map<String, List<ToolAgentContext>> grouped = allConfigs.stream()
                .collect(Collectors.groupingBy(ToolAgentContext::getContextType));

        // 1. 业务域划分
        List<ToolAgentContext> domains = grouped.get("business_domain");
        if (domains != null && !domains.isEmpty()) {
            sb.append("\n## 业务域划分（重要！参数体系不可混淆）\n");
            sb.append("系统分为两个独立业务域，唯一性规则和参数体系完全不同，不可混淆\n\n");
            for (ToolAgentContext ctx : domains) {
                JSONObject content = JSONObject.parseObject(ctx.getContent());
                sb.append("### ").append(content.getString("name")).append("\n");
                String scope = content.getString("scope");
                if (scope != null) sb.append("适用工具：").append(scope).append("\n");
                String uniqueness = content.getString("uniqueness");
                if (uniqueness != null) sb.append("数据唯一性：").append(uniqueness).append("\n");
                String paramDep = content.getString("param_dependency");
                if (paramDep != null) sb.append("参数依赖：").append(paramDep).append("\n");

                // uniqueness_rules
                JSONObject uniqRules = content.getJSONObject("uniqueness_rules");
                if (uniqRules != null) {
                    for (String key : uniqRules.keySet()) {
                        sb.append("- ").append(key).append("：").append(uniqRules.getString(key)).append("\n");
                    }
                }

                // key_facts
                JSONArray facts = content.getJSONArray("key_facts");
                if (facts != null) {
                    for (int i = 0; i < facts.size(); i++) {
                        sb.append("- ").append(facts.getString(i)).append("\n");
                    }
                }

                // typical_flow
                String flow = content.getString("typical_flow");
                if (flow != null) sb.append("典型流程：").append(flow).append("\n");
                String desc = content.getString("description");
                if (desc != null) sb.append(desc).append("\n");
                sb.append("\n");
            }
        }

        // 2. 实体关联关系
        List<ToolAgentContext> relations = grouped.get("entity_relation");
        if (relations != null && !relations.isEmpty()) {
            sb.append("## 实体关联关系\n");
            for (ToolAgentContext ctx : relations) {
                JSONObject content = JSONObject.parseObject(ctx.getContent());
                sb.append("- **").append(content.getString("from")).append("** → **");
                sb.append(content.getString("to")).append("**：");
                sb.append(content.getString("relation"));
                String linkField = content.getString("link_field");
                if (linkField != null) sb.append("（关联字段：").append(linkField).append("）");
                String queryTool = content.getString("query_tool");
                if (queryTool != null) sb.append("，查询工具：").append(queryTool);
                String desc = content.getString("description");
                if (desc != null) sb.append("。").append(desc);
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 3. 常见查询流程
        List<ToolAgentContext> flows = grouped.get("common_flow");
        if (flows != null && !flows.isEmpty()) {
            sb.append("## 常见查询流程\n");
            for (int i = 0; i < flows.size(); i++) {
                JSONObject content = JSONObject.parseObject(flows.get(i).getContent());
                sb.append("### ").append(i + 1).append(". ").append(content.getString("name")).append("\n");
                sb.append(content.getString("description")).append("\n");
                JSONArray steps = content.getJSONArray("steps");
                if (steps != null) {
                    for (int j = 0; j < steps.size(); j++) {
                        JSONObject step = steps.getJSONObject(j);
                        sb.append("  ").append(step.getIntValue("step")).append(". ");
                        sb.append("**").append(step.getString("tool")).append("**");
                        sb.append("：").append(step.getString("purpose"));
                        String inputField = step.getString("input_field");
                        if (inputField != null) {
                            sb.append("（从步骤").append(step.get("input_from_step")).append("获取").append(inputField).append("）");
                        }
                        sb.append("\n");
                    }
                }
            }
        }

        return sb.toString();
    }
}
