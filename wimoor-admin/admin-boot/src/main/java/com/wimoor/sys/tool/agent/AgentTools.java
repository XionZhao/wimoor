package com.wimoor.sys.tool.agent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wimoor.sys.tool.service.IToolAgentContextService;
import com.wimoor.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI Agent 工具定义
 * 从 resources/agent-tools/*.json 配置文件加载所有工具定义
 * 上下文配置优先从数据库加载，数据库无数据时fallback到00-context.json文件
 *
 * JSON配置文件按模块分文件存储，每个文件格式：
 * {
 *   "module": "模块名",
 *   "description": "模块描述",
 *   "tools": [
 *     {
 *       "name": "工具名",
 *       "description": "工具描述",
 *       "endpoint": "接口路径",
 *       "service": "微服务名",
 *       "params": { "参数名": {"type": "类型", "description": "描述"} },
 *       "required": ["必填参数"],
 *       "response_fields": { "字段名": "字段描述" }
 *     }
 *   ]
 * }
 * 
 * {
  "name": "工具名",
  "description": "工具描述",
  "endpoint": "接口路径",
  "service": "微服务名",
  "params": { "参数名": {"type": "类型", "description": "描述"} },
  "required": ["必填参数"],
  "response_fields": { "字段名": "字段说明" }
}
 */
public class AgentTools {

    private static List<JSONObject> allTools;
    private static List<JSONObject> allRawToolDefs; // 原始工具定义（含endpoint/method/service/module），用于getToolInfo
    private static final Object lock = new Object();

    /**
     * 清除缓存（用于重新加载工具配置）
     */
    public static void clearCache() {
        synchronized (lock) {
            allTools = null;
            allRawToolDefs = null;
        }
    }

    /**
     * 获取所有工具定义（OpenAI兼容格式）
     */
    public static List<JSONObject> getAllTools() {
        if (allTools != null) {
            return allTools;
        }
        synchronized (lock) {
            if (allTools != null) {
                return allTools;
            }
            allTools = loadToolsFromConfig();
            return allTools;
        }
    }

    /**
     * 根据工具名获取工具的详细信息（description, endpoint, method, service）
     * 用于前端展示AI调用了哪些接口
     * 复用缓存数据，避免重复扫描文件
     */
    public static JSONObject getToolInfo(String toolName) {
        // 确保缓存已加载
        getAllTools();
        if (allRawToolDefs == null) return null;

        for (JSONObject toolDef : allRawToolDefs) {
            if (toolName.equals(toolDef.getString("name"))) {
                JSONObject info = new JSONObject();
                info.put("name", toolName);
                info.put("description", toolDef.getString("description"));
                info.put("endpoint", toolDef.getString("endpoint"));
                info.put("method", toolDef.getString("method"));
                info.put("service", toolDef.getString("service"));
                info.put("module", toolDef.getString("_module"));
                return info;
            }
        }
        return null;
    }

    /**
     * 获取精简的工具列表（只有名称和描述），用于第一阶段AI选择工具
     * 返回格式：[{name: "tool_name", description: "工具描述"}, ...]
     */
    public static List<JSONObject> getToolSummaries() {
        return getToolSummaries(null);
    }

    /**
     * 获取精简的工具列表（按菜单权限过滤）
     * 只返回用户有权限访问的菜单对应的工具
     *
     * @param allowedMenuIds 用户有权限访问的菜单ID集合，null或空表示不过滤（返回全部）
     * @return 过滤后的工具摘要列表
     */
    public static List<JSONObject> getToolSummaries(Set<Integer> allowedMenuIds) {
        List<JSONObject> summaries = new ArrayList<>();
        if (allRawToolDefs == null) {
            getAllTools();
        }
        if (allRawToolDefs != null) {
            for (JSONObject toolDef : allRawToolDefs) {
                // 按菜单权限过滤：工具必须配置了menu_id，且在用户权限范围内
                if (!isToolAllowed(toolDef, allowedMenuIds)) {
                    continue;
                }
                JSONObject summary = new JSONObject();
                summary.put("name", toolDef.getString("name"));
                summary.put("description", toolDef.getString("description"));
                summaries.add(summary);
            }
        }
        return summaries;
    }

    /**
     * 根据工具名称列表获取完整的工具定义
     * 用于第二阶段只发送AI选择的工具
     */
    public static List<JSONObject> getToolsByNames(List<String> toolNames) {
        List<JSONObject> selectedTools = new ArrayList<>();
        if (allTools == null) {
            getAllTools();
        }
        if (allTools != null && toolNames != null) {
            Set<String> nameSet = new HashSet<>(toolNames);
            for (JSONObject tool : allTools) {
                JSONObject function = tool.getJSONObject("function");
                if (function != null && nameSet.contains(function.getString("name"))) {
                    selectedTools.add(tool);
                }
            }
        }
        return selectedTools;
    }

    /**
     * 从 resources/agent-tools/*.json 加载所有工具定义
     * 同时缓存原始工具定义（含endpoint/method/service/module），供getToolInfo使用
     * 使用文件名去重，避免从多个classpath位置重复加载同一文件
     */
    private static List<JSONObject> loadToolsFromConfig() {
        List<JSONObject> tools = new ArrayList<>();
        List<JSONObject> rawDefs = new ArrayList<>();
        Set<String> processedFiles = new HashSet<>(); // 文件名去重
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:agent-tools/*.json");
            System.out.println("[AgentTools] 扫描到 " + resources.length + " 个配置文件资源");

            // 按文件名排序，保证加载顺序一致
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                // 文件名去重：同一个文件只处理一次
                if (processedFiles.contains(fileName)) {
                    System.out.println("[AgentTools] 跳过重复文件: " + fileName);
                    continue;
                }
                processedFiles.add(fileName);
                System.out.println("[AgentTools] 加载文件: " + fileName + ", URI: " + resource.getURI());

                try (InputStream is = resource.getInputStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject moduleConfig = JSONObject.parseObject(json);
                    String moduleName = moduleConfig.getString("module");
                    // 从根级别获取默认service（模块级别）
                    String moduleService = moduleConfig.getString("service");
                    JSONArray moduleTools = moduleConfig.getJSONArray("tools");
                    if (moduleTools != null) {
                        for (int i = 0; i < moduleTools.size(); i++) {
                            JSONObject toolDef = moduleTools.getJSONObject(i);

                            // 如果工具级别没有service，使用模块级别的service
                            if (toolDef.getString("service") == null || toolDef.getString("service").isEmpty()) {
                                toolDef.put("service", moduleService);
                            }
                            tools.add(convertToOpenAIFormat(toolDef));
                            // 缓存原始定义，附加module信息
                            toolDef.put("_module", moduleName);
                            rawDefs.add(toolDef);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("加载工具配置文件失败: " + resource.getFilename() + ", 错误: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("扫描工具配置文件失败: " + e.getMessage());
        }
        allRawToolDefs = rawDefs; // 缓存原始定义
        System.out.println("[AgentTools] 加载完成，共 " + tools.size() + " 个工具，处理了 " + processedFiles.size() + " 个文件");
        return tools;
    }

    /**
     * 将JSON工具定义转换为OpenAI兼容格式
     */
    @SuppressWarnings("unchecked")
    private static JSONObject convertToOpenAIFormat(JSONObject toolDef) {
        JSONObject function = new JSONObject();
        function.put("name", toolDef.getString("name"));
        function.put("description", buildDescription(toolDef));

        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");

        JSONObject properties = new JSONObject();
        JSONObject params = toolDef.getJSONObject("params");
        if (params != null) {
            for (String key : params.keySet()) {
                JSONObject param = params.getJSONObject(key);
                JSONObject prop = new JSONObject();
                prop.put("type", param.getString("type"));
                prop.put("description", param.getString("description"));
                properties.put(key, prop);
            }
        }
        parameters.put("properties", properties);

        List<String> required = toolDef.getJSONArray("required") != null ?
                toolDef.getJSONArray("required").toJavaList(String.class) : Collections.emptyList();
        if (!required.isEmpty()) {
            parameters.put("required", required);
        }

        function.put("parameters", parameters);

        JSONObject tool = new JSONObject();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    /**
     * 构建工具描述，包含接口路径和返回字段信息
     */
    private static String buildDescription(JSONObject toolDef) {
        StringBuilder sb = new StringBuilder();
        sb.append(toolDef.getString("description"));
        sb.append(" 对应接口：").append(toolDef.getString("endpoint"));

        // 添加HTTP方法信息
        String method = toolDef.getString("method");
        if (method != null && !method.isEmpty()) {
            sb.append("（").append(method).append("）");
        }

        // 添加返回字段信息
        JSONObject responseFields = toolDef.getJSONObject("response_fields");
        if (responseFields != null && !responseFields.isEmpty()) {
            sb.append("\n返回字段：");
            for (String field : responseFields.keySet()) {
                sb.append("\n  - ").append(field).append("：").append(responseFields.getString(field));
            }
        }
        return sb.toString();
    }

    /**
     * 获取工具描述文本（用于系统提示词）
     */
    public static String getToolDescription() {
        return getToolDescription(null);
    }

    /**
     * 获取工具描述（按菜单权限过滤）
     * @param allowedMenuIds 用户有权限访问的菜单ID集合，null表示不过滤（返回全部）
     */
    public static String getToolDescription(Set<Integer> allowedMenuIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Wimoor ERP系统可用工具说明\n\n");

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:agent-tools/*.json");
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            Set<String> processedFiles = new HashSet<>(); // 文件名去重
            String currentModule = "";
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                // 文件名去重：同一个文件只处理一次
                if (processedFiles.contains(fileName)) {
                    continue;
                }
                processedFiles.add(fileName);

                try (InputStream is = resource.getInputStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject moduleConfig = JSONObject.parseObject(json);
                    JSONArray tools = moduleConfig.getJSONArray("tools");
                    if (tools == null || tools.isEmpty()) {
                        continue; // 跳过非工具定义文件（如00-context.json）
                    }

                    String moduleName = moduleConfig.getString("module");
                    String moduleDesc = moduleConfig.getString("description");

                    if (!Objects.equals(currentModule, moduleName)) {
                        sb.append("## ").append(moduleName).append("\n");
                        if (moduleDesc != null && !moduleDesc.isEmpty()) {
                            sb.append(moduleDesc).append("\n\n");
                        }
                        currentModule = moduleName;
                    }

                    for (int i = 0; i < tools.size(); i++) {
                        JSONObject tool = tools.getJSONObject(i);

                        // 权限过滤：检查工具是否有权限访问
                        Integer menuId = tool.getInteger("menu_id");
                        if (allowedMenuIds != null && !allowedMenuIds.isEmpty() && menuId != null && !allowedMenuIds.contains(menuId)) {
                            continue; // 跳过无权限的工具
                        }

                        sb.append("- **").append(tool.getString("name")).append("**：");
                        sb.append(tool.getString("description"));
                        sb.append("（").append(tool.getString("endpoint"));
                        String toolMethod = tool.getString("method");
                        if (toolMethod != null && !toolMethod.isEmpty()) {
                            sb.append(" ").append(toolMethod);
                        }
                        sb.append("）");

                        // 列出关键返回字段
                        JSONObject respFields = tool.getJSONObject("response_fields");
                        if (respFields != null && !respFields.isEmpty()) {
                            sb.append("\n  返回字段：");
                            int count = 0;
                            for (String field : respFields.keySet()) {
                                if (count > 0) sb.append("、");
                                sb.append(field).append("(").append(respFields.getString(field)).append(")");
                                count++;
                                if (count >= 10) {
                                    sb.append(" 等");
                                    break;
                                }
                            }
                        }
                        sb.append("\n");
                    }
                    sb.append("\n");
                } catch (Exception e) {
                    // 跳过加载失败的文件
                }
            }
        } catch (Exception e) {
            sb.append("工具描述加载失败\n");
        }

        sb.append("\n## 使用说明\n");
        sb.append("- 调用工具时，只需传入params中定义的参数，shopid(租户ID)会由系统自动注入\n");
        sb.append("- 如果查询结果为空，可能是参数不正确，请检查店铺分组ID、站点ID等是否正确\n");
        sb.append("- 可以通过查询店铺列表、站点列表等工具获取正确的ID值\n");
        sb.append("- 涉及金额的字段，注意币种(currency)字段的含义\n");

        // 从00-context.json加载关联关系配置
        sb.append(loadContextDescription());

        return sb.toString();
    }

    /**
     * 加载上下文关联关系描述
     * 优先从数据库加载（t_sys_tool_agent_context），数据库无数据时fallback到00-context.json文件
     */
    private static String loadContextDescription() {
        // 优先从数据库加载
        try {
            IToolAgentContextService contextService = SpringUtil.getBean(IToolAgentContextService.class);
            String dbResult = contextService.loadContextDescription();
            if (dbResult != null && !dbResult.isEmpty()) {
                return dbResult;
            }
        } catch (Exception e) {
            // 数据库加载失败，fallback到JSON文件
        }

        // fallback：从 agent-tools/00-context.json 加载
        StringBuilder sb = new StringBuilder();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:agent-tools/00-context.json");
            if (!resource.exists()) {
                return "";
            }
            try (InputStream is = resource.getInputStream()) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject ctx = JSONObject.parseObject(json);

                // 1. 实体关联关系
                JSONArray relations = ctx.getJSONArray("entity_relations");
                if (relations != null && !relations.isEmpty()) {
                    sb.append("\n## 实体关联关系\n");
                    for (int i = 0; i < relations.size(); i++) {
                        JSONObject rel = relations.getJSONObject(i);
                        sb.append("- **").append(rel.getString("from")).append("** → **");
                        sb.append(rel.getString("to")).append("**：");
                        sb.append(rel.getString("relation"));
                        String linkField = rel.getString("link_field");
                        if (linkField != null) sb.append("（关联字段：").append(linkField).append("）");
                        String queryTool = rel.getString("query_tool");
                        if (queryTool != null) sb.append("，查询工具：").append(queryTool);
                        sb.append("\n");
                    }
                }

                // 2. 字段别名映射
                JSONArray aliases = ctx.getJSONArray("field_aliases");
                if (aliases != null && !aliases.isEmpty()) {
                    sb.append("\n## 字段别名说明\n");
                    for (int i = 0; i < aliases.size(); i++) {
                        JSONObject alias = aliases.getJSONObject(i);
                        sb.append("- **").append(alias.getString("description")).append("**：");
                        JSONArray aliasList = alias.getJSONArray("aliases");
                        if (aliasList != null) {
                            for (int j = 0; j < aliasList.size(); j++) {
                                if (j > 0) sb.append("/");
                                sb.append(aliasList.getString(j));
                            }
                        }
                        String canonical = alias.getString("canonical");
                        if (canonical != null) sb.append(" → 标准名：").append(canonical);
                        String obtained = alias.getString("obtained_from");
                        if (obtained != null) sb.append("（获取方式：").append(obtained).append("）");
                        sb.append("\n");
                    }
                }

                // 3. 业务域划分（区分Amazon域和ERP域的参数体系）
                JSONObject domains = ctx.getJSONObject("business_domains");
                if (domains != null) {
                    sb.append("\n## 业务域划分（重要！参数体系不可混淆）\n");
                    String domainDesc = domains.getString("description");
                    if (domainDesc != null) sb.append(domainDesc).append("\n\n");

                    // Amazon域
                    JSONObject amazonDomain = domains.getJSONObject("amazon_domain");
                    if (amazonDomain != null) {
                        sb.append("### ").append(amazonDomain.getString("name")).append("\n");
                        sb.append("适用工具：").append(amazonDomain.getString("scope")).append("\n");
                        sb.append("参数依赖：").append(amazonDomain.getString("param_dependency")).append("\n");
                        JSONArray amazonFacts = amazonDomain.getJSONArray("key_facts");
                        if (amazonFacts != null) {
                            for (int i = 0; i < amazonFacts.size(); i++) {
                                sb.append("- ").append(amazonFacts.getString(i)).append("\n");
                            }
                        }
                        sb.append("\n");
                    }

                    // ERP域
                    JSONObject erpDomain = domains.getJSONObject("erp_domain");
                    if (erpDomain != null) {
                        sb.append("### ").append(erpDomain.getString("name")).append("\n");
                        sb.append("适用工具：").append(erpDomain.getString("scope")).append("\n");
                        sb.append("参数依赖：").append(erpDomain.getString("param_dependency")).append("\n");
                        JSONArray erpFacts = erpDomain.getJSONArray("key_facts");
                        if (erpFacts != null) {
                            for (int i = 0; i < erpFacts.size(); i++) {
                                sb.append("- ").append(erpFacts.getString(i)).append("\n");
                            }
                        }
                        sb.append("\n");
                    }

                    // 跨域关联
                    JSONObject crossDomain = domains.getJSONObject("cross_domain");
                    if (crossDomain != null) {
                        sb.append("### ").append(crossDomain.getString("name")).append("\n");
                        sb.append(crossDomain.getString("description")).append("\n");
                        String typicalFlow = crossDomain.getString("typical_flow");
                        if (typicalFlow != null) {
                            sb.append("典型流程：").append(typicalFlow).append("\n");
                        }
                    }
                }

                // 4. 常见查询流程
                JSONArray flows = ctx.getJSONArray("common_flows");
                if (flows != null && !flows.isEmpty()) {
                    sb.append("\n## 常见查询流程\n");
                    for (int i = 0; i < flows.size(); i++) {
                        JSONObject flow = flows.getJSONObject(i);
                        sb.append("### ").append(i + 1).append(". ").append(flow.getString("name")).append("\n");
                        sb.append(flow.getString("description")).append("\n");
                        JSONArray steps = flow.getJSONArray("steps");
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

                // 5. 页面上下文提示
                JSONArray hints = ctx.getJSONArray("page_context_hints");
                if (hints != null && !hints.isEmpty()) {
                    sb.append("\n## 页面对应的工具组合\n");
                    for (int i = 0; i < hints.size(); i++) {
                        JSONObject hint = hints.getJSONObject(i);
                        sb.append("- **").append(hint.getString("page")).append("**：");
                        JSONArray relatedTools = hint.getJSONArray("related_tools");
                        if (relatedTools != null) {
                            for (int j = 0; j < relatedTools.size(); j++) {
                                if (j > 0) sb.append("、");
                                sb.append(relatedTools.getString(j));
                            }
                        }
                        String note = hint.getString("note");
                        if (note != null) {
                            sb.append("（").append(note).append("）");
                        }
                        sb.append("\n");
                    }
                }

                // 6. 页面导航指南（用于指导用户操作）
                JSONObject navGuide = ctx.getJSONObject("page_navigation_guide");
                if (navGuide != null) {
                    sb.append("\n## 页面导航指南（指导用户进入具体页面）\n");
                    JSONArray modules = navGuide.getJSONArray("modules");
                    if (modules != null) {
                        for (int i = 0; i < modules.size(); i++) {
                            JSONObject module = modules.getJSONObject(i);
                            sb.append("\n### ").append(module.getString("name")).append("\n");
                            JSONArray pages = module.getJSONArray("pages");
                            if (pages != null) {
                                for (int j = 0; j < pages.size(); j++) {
                                    JSONObject page = pages.getJSONObject(j);
                                    sb.append("- **").append(page.getString("name")).append("**\n");
                                    sb.append("  菜单路径：").append(page.getString("menu_path")).append("\n");
                                    sb.append("  功能：").append(page.getString("description")).append("\n");
                                    JSONArray keywords = page.getJSONArray("keywords");
                                    if (keywords != null && !keywords.isEmpty()) {
                                        sb.append("  关键词：");
                                        for (int k = 0; k < keywords.size(); k++) {
                                            if (k > 0) sb.append("、");
                                            sb.append(keywords.getString(k));
                                        }
                                        sb.append("\n");
                                    }
                                    JSONArray features = page.getJSONArray("key_features");
                                    if (features != null && !features.isEmpty()) {
                                        sb.append("  主要功能：");
                                        for (int k = 0; k < features.size(); k++) {
                                            if (k > 0) sb.append("、");
                                            sb.append(features.getString(k));
                                        }
                                        sb.append("\n");
                                    }
                                }
                            }
                        }
                    }

                    // 操作指南
                    JSONObject opGuides = navGuide.getJSONObject("operation_guides");
                    if (opGuides != null) {
                        sb.append("\n### 常见操作指南\n");
                        JSONArray guides = opGuides.getJSONArray("guides");
                        if (guides != null) {
                            for (int i = 0; i < guides.size(); i++) {
                                JSONObject guide = guides.getJSONObject(i);
                                sb.append("\n**").append(guide.getString("name")).append("**\n");
                                sb.append("场景：").append(guide.getString("scenario")).append("\n");
                                sb.append("操作步骤：\n");
                                JSONArray steps = guide.getJSONArray("steps");
                                if (steps != null) {
                                    for (int j = 0; j < steps.size(); j++) {
                                        sb.append("  ").append(j + 1).append(". ").append(steps.getString(j)).append("\n");
                                    }
                                }
                                JSONArray tips = guide.getJSONArray("tips");
                                if (tips != null && !tips.isEmpty()) {
                                    sb.append("  提示：");
                                    for (int j = 0; j < tips.size(); j++) {
                                        if (j > 0) sb.append("；");
                                        sb.append(tips.getString(j));
                                    }
                                    sb.append("\n");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 上下文加载失败不影响工具使用
        }
        return sb.toString();
    }

    /**
     * 根据工具名称列表构建文本格式的工具定义提示词
     * 包含工具名称、描述、参数定义，供AI生成结构化工具调用
     */
    public static String buildToolPromptForNames(List<String> toolNames) {
        if (allRawToolDefs == null) {
            getAllTools();
        }
        StringBuilder sb = new StringBuilder();
        Set<String> nameSet = new HashSet<>(toolNames);
        for (JSONObject toolDef : allRawToolDefs) {
            if (nameSet.contains(toolDef.getString("name"))) {
                sb.append("### ").append(toolDef.getString("name")).append("\n");
                sb.append("描述：").append(toolDef.getString("description")).append("\n");
                sb.append("接口：").append(toolDef.getString("endpoint")).append("\n");
                sb.append("方法：").append(toolDef.getString("method")).append("\n");

                // 参数定义
                JSONObject params = toolDef.getJSONObject("params");
                if (params != null && !params.isEmpty()) {
                    sb.append("参数：\n");
                    for (String key : params.keySet()) {
                        JSONObject param = params.getJSONObject(key);
                        sb.append("  - ").append(key).append("(").append(param.getString("type")).append(")");
                        sb.append("：").append(param.getString("description"));
                        sb.append("\n");
                    }
                }

                // 必填参数
                JSONArray required = toolDef.getJSONArray("required");
                if (required != null && !required.isEmpty()) {
                    sb.append("必填参数：").append(required.toJSONString()).append("\n");
                }

                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 判断工具是否允许访问
     * 规则：
     * 1. 如果 allowedMenuIds 为 null，不过滤（管理员或获取权限异常时的降级策略）
     * 2. 如果 allowedMenuIds 为空集合，拒绝所有工具（用户没有任何菜单权限）
     * 3. 工具未配置 menu_id 时，默认拒绝（安全优先，避免未配置权限的工具被越权访问）
     * 4. 工具配置了 menu_id 时，必须在 allowedMenuIds 中才允许
     */
    private static boolean isToolAllowed(JSONObject toolDef, Set<Integer> allowedMenuIds) {
        // null表示不过滤（管理员用户或获取权限异常时的降级）
        if (allowedMenuIds == null) {
            return true;
        }
        // 空集合表示用户没有任何菜单权限，拒绝所有工具
        if (allowedMenuIds.isEmpty()) {
            return false;
        }
        // 工具未配置 menu_id，默认拒绝（安全优先）
        Integer menuId = toolDef.getInteger("menu_id");
        if (menuId == null) {
            return false;
        }
        // 工具配置了 menu_id，检查是否在用户权限范围内
        return allowedMenuIds.contains(menuId);
    }
}
