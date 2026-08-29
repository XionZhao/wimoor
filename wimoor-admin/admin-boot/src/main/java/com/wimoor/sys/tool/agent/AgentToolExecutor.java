package com.wimoor.sys.tool.agent;

import cn.hutool.core.net.URLEncodeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * AI Agent 工具执行器（微服务HTTP调用版）
 * 从 resources/agent-tools/*.json 配置文件加载端点映射
 * 通过 RestTemplate（@LoadBalanced）调用各微服务的API接口
 */
@Slf4j
@Component
public class AgentToolExecutor {

    // 工具端点映射：工具名 -> ToolEndpoint（从JSON配置文件加载）
    private static Map<String, ToolEndpoint> endpointMap;
    private static final Object lock = new Object();

    /**
     * HTTP方法常量
     */
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";

    /**
     * 应用启动时清除端点缓存，确保加载最新的配置文件
     */
    @javax.annotation.PostConstruct
    public void init() {
        clearCache();
        log.info("[AgentToolExecutor] 端点缓存已清除，将在首次使用时重新加载");
    }

    /**
     * 清除缓存（用于重新加载端点配置）
     */
    public static void clearCache() {
        synchronized (lock) {
            endpointMap = null;
        }
    }

    /**
     * 获取端点映射（懒加载）
     */
    private static Map<String, ToolEndpoint> getEndpointMap() {
        if (endpointMap != null) {
            return endpointMap;
        }
        synchronized (lock) {
            if (endpointMap != null) {
                return endpointMap;
            }
            endpointMap = loadEndpointMap();
            return endpointMap;
        }
    }

    /**
     * 从 resources/agent-tools/*.json 加载端点映射
     */
    private static Map<String, ToolEndpoint> loadEndpointMap() {
        Map<String, ToolEndpoint> map = new LinkedHashMap<>();
        Set<String> processedFiles = new HashSet<>(); // 文件名去重
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:agent-tools/*.json");
            System.out.println("[AgentToolExecutor] 扫描到 " + resources.length + " 个配置文件资源");
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                System.out.println("[AgentToolExecutor] 处理文件: " + fileName);
                // 文件名去重：同一个文件只处理一次
                if (processedFiles.contains(fileName)) {
                    continue;
                }
                processedFiles.add(fileName);

                try (InputStream is = resource.getInputStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject moduleConfig = JSONObject.parseObject(json);

                    // 从根级别获取默认service（模块级别）
                    String moduleService = moduleConfig.getString("service");

                    JSONArray tools = moduleConfig.getJSONArray("tools");
                    int toolCount = 0;
                    if (tools != null) {
                        for (int i = 0; i < tools.size(); i++) {
                            JSONObject tool = tools.getJSONObject(i);
                            String name = tool.getString("name");
                            // 如果已存在同名工具，跳过（去重）
                            if (map.containsKey(name)) {
                                continue;
                            }
                            String endpoint = tool.getString("endpoint");
                            // 优先使用工具级别的service，如果没有则使用模块级别的service
                            String service = tool.getString("service");
                            if (service == null || service.isEmpty()) {
                                service = moduleService;
                            }
                            String method = tool.getString("method");
                            if (method == null || method.isEmpty()) {
                                method = METHOD_GET; // 默认GET
                            }
                            if (name != null && endpoint != null && service != null) {
                                JSONObject defaultParams = tool.getJSONObject("default_params");
                                map.put(name, new ToolEndpoint(service, endpoint, method, defaultParams));
                                toolCount++;
                            } else {
                                System.out.println("[AgentToolExecutor] 跳过工具: name=" + name + ", endpoint=" + endpoint + ", service=" + service + ", 来自文件=" + fileName);
                            }
                        }
                    }
                    System.out.println("[AgentToolExecutor] 文件 " + fileName + " 加载了 " + toolCount + " 个工具");
                } catch (Exception e) {
                    System.err.println("加载端点配置失败: " + resource.getFilename() + ", 错误: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("扫描端点配置文件失败: " + e.getMessage());
        }
        System.out.println("[AgentToolExecutor] 加载完成，共 " + map.size() + " 个工具端点，处理了 " + processedFiles.size() + " 个文件");
        return map;
    }

    /**
     * 执行工具调用
     */
    @SuppressWarnings("unchecked")
    public String execute(String toolName, String arguments, UserInfo userInfo) {
        log.info("[ToolExecutor] 执行工具调用: {}, 参数: {}", toolName, arguments);
        log.info("[ToolExecutor] 已注册工具数量: {}", getEndpointMap().size());
        ToolEndpoint endpoint = getEndpointMap().get(toolName);

        // 如果工具不存在，尝试模糊匹配
        if (endpoint == null) {
            String matchedName = fuzzyMatchTool(toolName);
            if (matchedName != null) {
                endpoint = getEndpointMap().get(matchedName);
                log.info("[ToolExecutor] 工具名称模糊匹配: {} -> {}", toolName, matchedName);
            }
        }

        if (endpoint == null) {
            // 输出详细的调试信息
            Map<String, ToolEndpoint> map = getEndpointMap();
            List<String> toolNames = new ArrayList<>(map.keySet());
            Collections.sort(toolNames);
            log.warn("[ToolExecutor] 工具不存在: {}", toolName);
            log.warn("[ToolExecutor] 已注册工具数: {}, 前20个: {}", map.size(), toolNames.subList(0, Math.min(20, toolNames.size())));
            // 检查是否包含类似名称
            for (String name : toolNames) {
                if (name.contains("fba_inventory") || name.contains("amz_query_fba")) {
                    log.info("[ToolExecutor] 找到相似工具: {}", name);
                }
            }
            return buildToolNotFoundResponse(toolName);
        }

        log.info("[ToolExecutor] 工具匹配成功: {} -> service={}, endpoint={}", toolName, endpoint.service, endpoint.path);

        // 内置系统工具：直接返回结果，不需要HTTP调用
        if ("system".equals(endpoint.service)) {
            return executeBuiltInTool(toolName);
        }

        try {
            JSONObject args = JSON.parseObject(arguments != null ? arguments : "{}");

            // 注入默认参数（AI未传时自动补充，如isDelete=0）
            if (endpoint.defaultParams != null) {
                for (String key : endpoint.defaultParams.keySet()) {
                    if (!args.containsKey(key) || args.get(key) == null || args.getString(key).isEmpty()) {
                        args.put(key, endpoint.defaultParams.get(key));
                    }
                }
            }

            // 注入shopid到参数中（租户隔离，由系统自动注入）
            args.put("shopid", userInfo.getCompanyid());

            // 构建完整URL：http://{service-name}{path}
            String url = "http://" + endpoint.service + endpoint.path;

            // 使用 RestTemplate 调用微服务
            RestTemplate restTemplate = SpringUtil.getBean("restTemplateApi");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 关键：将用户信息序列化后添加到请求头，与前端请求保持一致
            // 后端微服务通过 UserInfoFilter 从 X-USERINFO 请求头解析用户信息到 ThreadLocal
            String jsonUser = JSONObject.toJSONString(userInfo);
            jsonUser = URLEncodeUtil.encode(jsonUser, Charset.forName("utf-8"));
            headers.set(UserInfoContext.HEADER_USER_INFO, jsonUser);

            ResponseEntity<String> response;
            HttpMethod httpMethod = METHOD_POST.equalsIgnoreCase(endpoint.method) ? HttpMethod.POST : HttpMethod.GET;

            if (httpMethod == HttpMethod.POST) {
                // POST请求：将参数作为请求体发送
                HttpEntity<String> requestEntity = new HttpEntity<>(args.toJSONString(), headers);
                log.info("Agent工具调用(POST): {} -> {} body={}", toolName, url, args.toJSONString());
                response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            } else {
                // GET请求：将参数作为查询参数拼接
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
                for (Map.Entry<String, Object> entry : args.entrySet()) {
                    Object value = entry.getValue();
                    if (value != null && !"".equals(value.toString())) {
                        builder.queryParam(entry.getKey(), value);
                    }
                }
                String finalUrl = builder.toUriString();
                log.info("Agent工具调用(GET): {} -> {}", toolName, finalUrl);
                response = restTemplate.exchange(finalUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            }

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                return buildErrorResponse("接口返回异常: HTTP " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("执行工具 {} 失败: {}", toolName, e.getMessage(), e);
            return buildErrorResponse("工具执行失败: " + e.getMessage());
        }
    }

    // ========== 内置系统工具 ==========

    /**
     * 执行内置系统工具（不需要HTTP调用）
     */
    private String executeBuiltInTool(String toolName) {
        JSONObject result = new JSONObject();
        switch (toolName) {
            case "system_get_current_time":
                Calendar cal = Calendar.getInstance();
                result.put("success", true);
                result.put("datetime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime()));
                result.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
                result.put("year", cal.get(Calendar.YEAR));
                result.put("month", cal.get(Calendar.MONTH) + 1);
                result.put("day", cal.get(Calendar.DAY_OF_MONTH));
                result.put("hour", cal.get(Calendar.HOUR_OF_DAY));
                result.put("minute", cal.get(Calendar.MINUTE));
                String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
                result.put("dayOfWeek", weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]);
                result.put("timezone", java.util.TimeZone.getDefault().getID());
                log.info("[ToolExecutor] 系统工具执行: {} -> {}", toolName, result.toJSONString());
                return result.toJSONString();
            default:
                result.put("success", false);
                result.put("error", "未知的内置工具: " + toolName);
                return result.toJSONString();
        }
    }

    // ========== 辅助方法 ==========

    private String buildErrorResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("error", message);
        return response.toJSONString();
    }

    /**
     * 模糊匹配工具名称
     * 支持：忽略大小写、忽略下划线/连字符、包含匹配
     */
    private String fuzzyMatchTool(String toolName) {
        if (toolName == null || toolName.isEmpty()) {
            return null;
        }

        Map<String, ToolEndpoint> map = getEndpointMap();
        String normalizedInput = toolName.toLowerCase().replace("-", "_").replace(" ", "_");

        // 1. 精确匹配（忽略大小写）
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase(toolName)) {
                return key;
            }
        }

        // 2. 规范化后匹配（忽略下划线/连字符差异）
        for (String key : map.keySet()) {
            String normalizedKey = key.toLowerCase().replace("-", "_").replace(" ", "_");
            if (normalizedKey.equals(normalizedInput)) {
                return key;
            }
        }

        // 3. 包含匹配（输入包含在工具名中，或工具名包含在输入中）
        String bestMatch = null;
        int bestScore = 0;
        for (String key : map.keySet()) {
            String normalizedKey = key.toLowerCase().replace("-", "_").replace(" ", "_");
            if (normalizedKey.contains(normalizedInput) || normalizedInput.contains(normalizedKey)) {
                int score = Math.min(normalizedKey.length(), normalizedInput.length());
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = key;
                }
            }
        }

        return bestMatch;
    }

    /**
     * 构建工具未找到的错误响应，包含可用工具列表
     */
    private String buildToolNotFoundResponse(String toolName) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("error", "未知的工具: " + toolName);

        // 添加可用工具列表，帮助AI重试
        Map<String, ToolEndpoint> map = getEndpointMap();
        JSONArray availableTools = new JSONArray();
        for (String key : map.keySet()) {
            availableTools.add(key);
        }
        response.put("available_tools", availableTools);
        response.put("hint", "请使用上述可用工具名称重试，注意工具名称必须完全匹配");

        return response.toJSONString();
    }

    /**
     * 工具端点定义
     */
    private static class ToolEndpoint {
        final String service;  // 微服务名
        final String path;     // API路径（含context-path）
        final String method;   // HTTP方法（GET/POST）
        final JSONObject defaultParams; // 默认参数，AI未传时自动注入

        ToolEndpoint(String service, String path, String method, JSONObject defaultParams) {
            this.service = service;
            this.path = path;
            this.method = method;
            this.defaultParams = defaultParams;
        }
    }
}
