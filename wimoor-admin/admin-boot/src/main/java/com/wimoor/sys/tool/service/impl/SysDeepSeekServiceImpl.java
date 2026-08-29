package com.wimoor.sys.tool.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimoor.admin.common.exception.BizException;
import com.wimoor.admin.mapper.SysMenuMapper;
import com.wimoor.admin.pojo.entity.SysMenu;
import com.wimoor.common.GeneralUtil;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserType;
import com.wimoor.sys.tool.agent.AgentToolExecutor;
import com.wimoor.sys.tool.agent.AgentTools;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import com.wimoor.sys.tool.pojo.dto.SysChartCompletionRequestDTO;
import com.wimoor.sys.tool.pojo.entity.DeepSeekMessage;
import com.wimoor.sys.tool.pojo.entity.DeepseekChatMessages;
import com.wimoor.sys.tool.pojo.entity.DeepseekChatSessions;
import com.wimoor.sys.tool.pojo.entity.ToolAgentLearning;
import com.wimoor.sys.tool.service.IDeepseekChatMessagesService;
import com.wimoor.sys.tool.service.IDeepseekChatSessionsService;
import com.wimoor.sys.tool.service.IToolAgentLearningService;
import com.wimoor.sys.tool.service.ISysDeepSeekService;
import com.wimoor.util.SpringUtil;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeepSeekServiceImpl implements ISysDeepSeekService {
    final IDeepseekChatSessionsService iDeepseekChatSessionsService;
    final IDeepseekChatMessagesService iDeepseekChatMessagesService;

    @Autowired
    private AgentToolExecutor agentToolExecutor;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Value("${deepseek.token}")
    private String token;

    // 线程池用于异步处理流式请求
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 应用启动时清除工具缓存，确保加载最新的配置文件
     */
    @javax.annotation.PostConstruct
    public void init() {
        AgentTools.clearCache();
        log.info("[Agent] 工具缓存已清除，将在首次使用时重新加载");
    }

    public OkHttpClient getClient(){
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30, TimeUnit.SECONDS);   // 连接超时30秒
        builder.callTimeout(120, TimeUnit.SECONDS);      // 单次调用总超时2分钟
        builder.readTimeout(90, TimeUnit.SECONDS);       // 读取超时90秒
        builder.writeTimeout(30, TimeUnit.SECONDS);      // 写入超时30秒
        return builder.build();
    }
    public Response requestDeepSeek( RequestBody body ) throws IOException {
        Request request = new Request.Builder()
                .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer "+token)
                .build();
        OkHttpClient client=getClient();
        return client.newCall(request).execute();
    }

   public Object completions(UserInfo userInfo ,SysChartCompletionRequestDTO dto){
       try {
           Generation gen = new Generation();
           List<DeepseekChatMessages> messages = iDeepseekChatMessagesService.lambdaQuery()
                   .eq(DeepseekChatMessages::getSessionId, dto.getSessionId())
                   .orderBy(true,true,DeepseekChatMessages::getCreatetime)
                   .list();
           List<DeepseekChatMessages> newMessages = new ArrayList<DeepseekChatMessages>();
           List<Message> messageList=new ArrayList<Message>();
           for(DeepseekChatMessages message:messages){
               Message e=Message.builder()
                       .role(message.getRole())
                       .content(message.getContent())
                       .build();
               messageList.add(e);
           }
           for(DeepSeekMessage message:dto.getMessages()){
               DeepseekChatMessages e=new DeepseekChatMessages();
               e.setSessionId(dto.getSessionId());
               e.setCreatetime(new Date());
               e.setContent(message.getContent());
               e.setRole(message.getRole());
               newMessages.add(e);
               Message msg=Message.builder()
                       .role(message.getRole())
                       .content(message.getContent())
                       .build();
               messageList.add(msg);
           }
           GenerationParam param = GenerationParam.builder()
                   .apiKey(token)
                   .model(dto.getModel())
                   .enableThinking(true)
                   .incrementalOutput(false)
                   .resultFormat("message")
                   .messages(messageList)
                   .build();
           GenerationResult result = gen.call(param);
           String content = result.getOutput().getChoices().get(0).getMessage().getContent();
           String role = result.getOutput().getChoices().get(0).getMessage().getRole();
           log.info("[completions] AI返回: role={}, content长度={}, content={}", 
               role, content != null ? content.length() : 0, 
               content != null ? content.substring(0, Math.min(200, content.length())) : "null");
           DeepseekChatMessages e=new DeepseekChatMessages();
           e.setSessionId(dto.getSessionId());
           e.setCreatetime(new Date());
           e.setContent(content);
           e.setRole(role);
           newMessages.add(e);
           DeepseekChatSessions session=null;
           String sessionId = dto.getSessionId();
           if(StrUtil.isBlankOrUndefined(sessionId)){
               sessionId = UUID.randomUUID().toString();
               for(DeepseekChatMessages message:newMessages){
                   message.setSessionId(sessionId);
               }
               session=new DeepseekChatSessions();
               session.setId(sessionId);
               session.setUserid(userInfo.getId());
               session.setOpttime(new Date());
               session.setModel(dto.getModel());
               session.setCreatetime(new Date());
               session.setTitle(extractSessionTitle(dto.getMessages()));
               this.iDeepseekChatSessionsService.save(session);
           }else{
               session=iDeepseekChatSessionsService.getById(sessionId);
           }
           iDeepseekChatMessagesService.saveBatch(newMessages);
           if(messages!=null){
               messages.addAll(newMessages);
               session.setMessages(messages);
           }else{
               session.setMessages(newMessages);
           }
           return session;
       } catch (ApiException | NoApiKeyException | InputRequiredException e) {
           System.err.println("An exception occurred: " + e.getMessage());
           throw new BizException("请求失败: " + e.getMessage());
       } catch (Exception e) {
           log.error("[completions] 调用AI失败", e);
           throw new BizException("请求失败: " + e.getMessage());
       }
   }

    @Override
    public SseEmitter completionsStream(UserInfo userInfo, SysChartCompletionRequestDTO dto) {
        // 创建SseEmitter，设置超时时间
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 捕获当前线程的Shiro上下文，传播到异步线程
        org.apache.shiro.mgt.SecurityManager securityManager = null;
        Subject subject = null;
        try {
            securityManager = SecurityUtils.getSecurityManager();
            subject = SecurityUtils.getSubject();
        } catch (Exception e) {
            log.warn("获取Shiro上下文失败，将不传播Shiro上下文: {}", e.getMessage());
        }

        // 保存最终值用于lambda表达式
        final org.apache.shiro.mgt.SecurityManager finalSecurityManager = securityManager;
        final Subject finalSubject = subject;

        // 异步处理流式请求
        executorService.execute(() -> {
            try {
                // 绑定Shiro上下文到异步线程
                if (finalSecurityManager != null) {
                    ThreadContext.bind(finalSecurityManager);
                }
                if (finalSubject != null) {
                    ThreadContext.bind(finalSubject);
                }
                // 构建消息列表
                List<Message> messageList = new ArrayList<>();
                
                // 获取历史消息（验证会话归属当前用户，防止跨租户访问）
                if (StrUtil.isNotBlank(dto.getSessionId())) {
                    DeepseekChatSessions existingSession = iDeepseekChatSessionsService.lambdaQuery()
                            .eq(DeepseekChatSessions::getId, dto.getSessionId())
                            .eq(DeepseekChatSessions::getUserid, userInfo.getId())
                            .one();
                    if (existingSession == null) {
                        throw new RuntimeException("会话不存在或无权访问");
                    }
                    List<DeepseekChatMessages> historyMessages = iDeepseekChatMessagesService.lambdaQuery()
                            .eq(DeepseekChatMessages::getSessionId, dto.getSessionId())
                            .orderBy(true, true, DeepseekChatMessages::getCreatetime)
                            .list();
                    for (DeepseekChatMessages message : historyMessages) {
                        Message e = Message.builder()
                                .role(message.getRole())
                                .content(message.getContent())
                                .build();
                        messageList.add(e);
                    }
                }
                
                // 添加新消息
                List<DeepseekChatMessages> newMessages = new ArrayList<>();
                for (DeepSeekMessage message : dto.getMessages()) {
                    DeepseekChatMessages e = new DeepseekChatMessages();
                    e.setSessionId(dto.getSessionId());
                    e.setCreatetime(new Date());
                    e.setContent(message.getContent());
                    e.setRole(message.getRole());
                    newMessages.add(e);
                    
                    Message msg = Message.builder()
                            .role(message.getRole())
                            .content(message.getContent())
                            .build();
                    messageList.add(msg);
                }
                
                // 构建流式请求参数
                Generation gen = new Generation();
                GenerationParam param = GenerationParam.builder()
                        .apiKey(token)
                        .model(dto.getModel())
                        .enableThinking(false) // 关闭思考模式以提高响应速度
                        .incrementalOutput(true) // 启用增量输出
                        .resultFormat("message")
                        .messages(messageList)
                        .build();
                
                // 用于存储完整的AI回复和思考内容
                StringBuilder fullContent = new StringBuilder();
                StringBuilder fullReasoning = new StringBuilder();
                String sessionId = dto.getSessionId();
                
                // 调用流式API
                Flowable<GenerationResult> result = gen.streamCall(param);
                result.blockingForEach(message -> {
                    try {
                        String content = message.getOutput().getChoices().get(0).getMessage().getContent();
                        String reasoning = message.getOutput().getChoices().get(0).getMessage().getReasoningContent();
                        
                        boolean hasData = false;
                        JSONObject data = new JSONObject();
                        
                        // 发送思考内容
                        if (reasoning != null && !reasoning.isEmpty()) {
                            fullReasoning.append(reasoning);
                            data.put("reasoning", reasoning);
                            hasData = true;
                        }
                        
                        // 发送正式内容
                        if (content != null && !content.isEmpty()) {
                            fullContent.append(content);
                            data.put("content", content);
                            hasData = true;
                        }
                        
                        // 只在有数据时发送
                        if (hasData) {
                            String jsonData = data.toJSONString();
                            emitter.send(SseEmitter.event()
                                .id(String.valueOf(System.currentTimeMillis()))
                                .data(jsonData, org.springframework.http.MediaType.APPLICATION_JSON));
                        }
                    } catch (IOException e) {
                        log.error("发送SSE数据失败", e);
                    }
                });
                
                // 保存会话和消息
                DeepseekChatSessions session = null;
                if (StrUtil.isBlankOrUndefined(sessionId)) {
                    sessionId = UUID.randomUUID().toString();
                    for (DeepseekChatMessages message : newMessages) {
                        message.setSessionId(sessionId);
                    }
                    session = new DeepseekChatSessions();
                    session.setId(sessionId);
                    session.setUserid(userInfo.getId());
                    session.setOpttime(new Date());
                    session.setModel(dto.getModel());
                    session.setCreatetime(new Date());
                    session.setTitle(extractSessionTitle(dto.getMessages()));
                    this.iDeepseekChatSessionsService.save(session);
                } else {
                    session = iDeepseekChatSessionsService.getById(dto.getSessionId());
                }
                
                // 保存AI回复消息
                DeepseekChatMessages aiMessage = new DeepseekChatMessages();
                aiMessage.setSessionId(sessionId);
                aiMessage.setCreatetime(new Date());
                aiMessage.setContent(fullContent.toString());
                aiMessage.setReasoningContent(fullReasoning.toString()); // 保存思考内容
                aiMessage.setRole("assistant");
                newMessages.add(aiMessage);
                iDeepseekChatMessagesService.saveBatch(newMessages);
                
                // 发送完成事件，包含sessionId
                JSONObject completeData = new JSONObject();
                completeData.put("id", sessionId);
                completeData.put("content", "[DONE]");
                log.info("发送SSE完成事件: {}", completeData.toJSONString());
                emitter.send(SseEmitter.event()
                    .id("complete")
                    .data(completeData.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                
                emitter.complete();
            } catch (Exception e) {
                log.error("流式请求失败", e);
                try {
                    // 发送错误信息给前端
                    JSONObject errorData = new JSONObject();
                    errorData.put("error", e.getMessage());
                    emitter.send(SseEmitter.event().data(errorData.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                } catch (IOException ex) {
                    log.error("发送错误信息失败", ex);
                }
                emitter.completeWithError(e);
            } finally {
                // 清理Shiro上下文，防止内存泄漏
                ThreadContext.unbindSecurityManager();
                ThreadContext.unbindSubject();
            }
        });

        // 设置超时和错误回调
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时");
            emitter.complete();
        });
        
        emitter.onError(e -> {
            log.error("SSE连接错误", e);
        });
        
        return emitter;
    }

    /**
     * 获取用户有权限访问的菜单ID集合
     * admin用户返回null（不过滤，拥有全部权限）
     * 其他用户根据角色关联的菜单返回ID集合
     */
    private Set<Integer> getUserAllowedMenuIds(UserInfo userInfo) {
        // admin用户拥有全部权限，不需要过滤
        if (userInfo.getUsertype() != null && UserType.admin.getCode().equals(userInfo.getUsertype())) {
            return null;
        }
        try {
            List<SysMenu> menuList = sysMenuMapper.listRoute(userInfo.getId());
            if (menuList == null || menuList.isEmpty()) {
                return new HashSet<>();
            }
            Set<Integer> menuIds = new HashSet<>();
            for (SysMenu menu : menuList) {
                menuIds.add(Integer.parseInt(menu.getId()));
            }
            return menuIds;
        } catch (Exception e) {
            log.error("[AgentSync] 获取用户菜单权限失败，为安全起见拒绝所有工具访问: {}", e.getMessage(), e);
            return new HashSet<>();
        }
    }

    @Override
    public String completionsAgentSync(UserInfo userInfo, SysChartCompletionRequestDTO dto) {
        log.info("[AgentSync] 开始处理Agent同步请求, sessionId={}, userId={}", dto.getSessionId(), userInfo.getId());
        try {
            // 获取用户可访问的菜单ID集合，用于过滤Agent工具
            Set<Integer> allowedMenuIds = getUserAllowedMenuIds(userInfo);
            log.info("[AgentSync] 用户可访问菜单数: {}", allowedMenuIds != null ? allowedMenuIds.size() : "全部");

            // 构建消息列表
            List<JSONObject> messages = new ArrayList<>();
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", buildAgentSystemPrompt(userInfo, allowedMenuIds, dto.getCurrentPage(), dto.getPageTitle(), dto.getHelpDocUrl(), dto.getHelpDocLibrary(), dto.getCurrentHelpDoc()));
            messages.add(sysMsg);

            // 添加新消息
            for (DeepSeekMessage message : dto.getMessages()) {
                JSONObject msg = new JSONObject();
                msg.put("role", message.getRole());
                msg.put("content", message.getContent());
                messages.add(msg);
            }

            // 第一阶段：让AI选择需要使用的工具（按菜单权限过滤）
            List<JSONObject> toolSummaries = AgentTools.getToolSummaries(allowedMenuIds);
            List<String> selectedToolNames = selectToolsWithAI(messages, toolSummaries, dto.getModel());
            log.info("[AgentSync] AI选择了{}个工具: {}", selectedToolNames.size(), selectedToolNames);

            if (selectedToolNames.isEmpty()) {
                // AI没有选择任何工具，检查是否是业务查询
                String userQuestion = dto.getMessages() != null && !dto.getMessages().isEmpty() ?
                        dto.getMessages().get(dto.getMessages().size() - 1).getContent() : "";
                if (containsBusinessKeyword(userQuestion)) {
                    return "抱歉，您没有查询该数据的权限。如需开通相关功能，请联系管理员。";
                }
                JSONObject directResponse = callAIWithoutTools(messages, dto.getModel());
                String directContent = directResponse.getString("content");
                return StrUtil.isNotBlank(directContent) ? directContent : "抱歉，我无法理解您的问题。";
            }

            // 第二阶段：使用工具
            String toolPrompt = AgentTools.buildToolPromptForNames(selectedToolNames);
            String toolCallInstruction = buildToolCallInstruction(toolPrompt);
            JSONObject toolInstMsg = new JSONObject();
            toolInstMsg.put("role", "system");
            toolInstMsg.put("content", toolCallInstruction);
            messages.add(toolInstMsg);

            int maxIterations = 15;
            int iteration = 0;
            StringBuilder fullContent = new StringBuilder();

            while (iteration < maxIterations) {
                iteration++;
                log.info("[AgentSync] 第{}轮循环", iteration);

                JSONObject response = callAIWithoutTools(messages, dto.getModel());
                String assistantContent = response.getString("content");

                if (StrUtil.isBlank(assistantContent)) {
                    log.warn("[AgentSync] AI返回内容为空");
                    break;
                }

                // 检查是否有工具调用
                List<JSONObject> toolCalls = parseToolCallsFromText(assistantContent);
                if (toolCalls.isEmpty()) {
                    fullContent.append(assistantContent);
                    log.info("[AgentSync] 无工具调用，返回最终回复");
                    break;
                }

                // 执行工具调用
                log.info("[AgentSync] 检测到{}个工具调用", toolCalls.size());
                JSONObject assistantMsg = new JSONObject();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", assistantContent);
                messages.add(assistantMsg);

                // 构建已授权工具名称集合，用于二次校验
                Set<String> allowedToolNameSet = new HashSet<>(selectedToolNames);

                StringBuilder toolResultsText = new StringBuilder();
                for (JSONObject toolCall : toolCalls) {
                    String toolName = toolCall.getString("tool") != null ? toolCall.getString("tool") : toolCall.getString("name");
                    JSONObject arguments = toolCall.getJSONObject("params") != null ? toolCall.getJSONObject("params") : toolCall.getJSONObject("arguments");

                    // 权限二次校验：确保工具在第一阶段筛选通过的列表中
                    if (!allowedToolNameSet.contains(toolName)) {
                        log.warn("[AgentSync] 工具不在授权列表中，拒绝执行: {}", toolName);
                        toolResultsText.append("【工具 ").append(toolName).append(" 权限不足】\n");
                        toolResultsText.append("{\"success\":false,\"error\":\"权限不足：您没有使用该工具的权限。请联系管理员开通相应权限。\"}\n\n");
                        continue;
                    }

                    log.info("[AgentSync] 执行工具: {}", toolName);

                    try {
                        String resultStr = agentToolExecutor.execute(toolName, arguments.toJSONString(), userInfo);
                        toolResultsText.append("【工具 ").append(toolName).append(" 返回结果】\n");
                        toolResultsText.append(resultStr).append("\n\n");
                    } catch (Exception e) {
                        log.error("[AgentSync] 工具执行异常: {}", toolName, e);
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && errorMsg.contains("权限")) {
                            toolResultsText.append("【工具 ").append(toolName).append(" 权限不足】\n");
                            toolResultsText.append("{\"success\":false,\"error\":\"权限不足：").append(errorMsg).append("。请联系管理员开通相应权限。\"}\n\n");
                        } else {
                            toolResultsText.append("【工具 ").append(toolName).append(" 执行失败】\n");
                            toolResultsText.append("{\"success\":false,\"error\":\"工具执行失败：").append(errorMsg).append("\"}\n\n");
                        }
                    }
                }

                JSONObject toolResultMsg = new JSONObject();
                toolResultMsg.put("role", "system");
                toolResultMsg.put("content", "工具执行结果：\n" + toolResultsText.toString());
                messages.add(toolResultMsg);
            }

            if (fullContent.length() == 0) {
                log.warn("[AgentSync] Agent未生成回复，使用普通AI");
                JSONObject directResponse = callAIWithoutTools(messages, dto.getModel());
                return directResponse.getString("content") != null ? directResponse.getString("content") : "";
            }

            return fullContent.toString();

        } catch (Exception e) {
            log.error("[AgentSync] Agent同步请求失败", e);
            return "";
        }
    }

    public Object completionsOld(UserInfo userInfo ,SysChartCompletionRequestDTO dto){
        try {
            MediaType mediaType = MediaType.parse("application/json");
            ObjectMapper objectMapper = new ObjectMapper();
            List<DeepseekChatMessages> messages = iDeepseekChatMessagesService.lambdaQuery()
                    .eq(DeepseekChatMessages::getSessionId, dto.getSessionId())
                    .orderBy(true,true,DeepseekChatMessages::getCreatetime)
                    .list();
            List<DeepseekChatMessages> newMessages = new ArrayList<DeepseekChatMessages>();
            List<DeepSeekMessage> contentList=new ArrayList<DeepSeekMessage>();
            for(DeepseekChatMessages message:messages){
                DeepSeekMessage e=new DeepSeekMessage();
                e.setContent(message.getContent());
                e.setRole(message.getRole());
                contentList.add(e);
            }
            for(DeepSeekMessage message:dto.getMessages()){
                DeepseekChatMessages e=new DeepseekChatMessages();
                e.setSessionId(dto.getSessionId());
                e.setCreatetime(new Date());
                e.setContent(message.getContent());
                e.setRole(message.getRole());
                newMessages.add(e);
                contentList.add(message);
            }
            dto.setMessages(contentList);
            String jsonBody = objectMapper.writeValueAsString(dto);
            RequestBody body = RequestBody.create(mediaType, jsonBody);
            Response response = requestDeepSeek(body);
            if(response==null||!response.isSuccessful()||response.body()==null){
                throw new BizException("请求失败");
            }
            String result= response.body().string();
            JSONObject resultJson = GeneralUtil.getJsonObject(result);
            String sessionId = resultJson.getString("id");
            JSONArray choices = resultJson.getJSONArray("choices");
            for(int i=0;i<choices.size();i++){
                JSONObject message = choices.getJSONObject(i);
                JSONObject messageInfo = message.getJSONObject("message");
                String content= messageInfo.getString("content");
                String role= messageInfo.getString("role");
                DeepseekChatMessages e=new DeepseekChatMessages();
                e.setSessionId(dto.getSessionId());
                e.setCreatetime(new Date());
                e.setContent(content);
                e.setRole(role);
                newMessages.add(e);
            }
            DeepseekChatSessions session=null;
            if(StrUtil.isBlankOrUndefined(dto.getSessionId())){
                for(DeepseekChatMessages message:newMessages){
                    message.setSessionId(sessionId);
                }
                session=new DeepseekChatSessions();
                session.setId(sessionId);
                session.setUserid(userInfo.getId());
                session.setOpttime(new Date());
                session.setModel(dto.getModel());
                session.setCreatetime(new Date());
                session.setTitle(extractSessionTitle(dto.getMessages()));
                this.iDeepseekChatSessionsService.save(session);
            }else{
                resultJson.put("id", dto.getSessionId());
                session=iDeepseekChatSessionsService.getById(dto.getSessionId());
            }
            iDeepseekChatMessagesService.saveBatch(newMessages);
            if(messages!=null){
                messages.addAll(newMessages);
                session.setMessages(messages);
            }else{
                session.setMessages(newMessages);
            }
            return session;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getSession(UserInfo userInfo) {
        LambdaQueryChainWrapper<DeepseekChatSessions> query = iDeepseekChatSessionsService.lambdaQuery();
        query.eq(DeepseekChatSessions::getUserid, userInfo.getId());
        query.orderBy(true,false,DeepseekChatSessions::getCreatetime);
        IPage<DeepseekChatSessions> page=new Page<DeepseekChatSessions>();
        page.setCurrent(0);
        page.setSize(1000);
        query.page(page);
        List<DeepseekChatSessions> list = query.list();
        for(DeepseekChatSessions item:list){
            List<DeepseekChatMessages> messages = iDeepseekChatMessagesService.lambdaQuery().eq(DeepseekChatMessages::getSessionId, item.getId()).list();
            item.setMessages(messages);
        }
        return list;
    }

    @Override
    public void deleteSession(UserInfo userInfo, String sessionId) {
        // 删除会话下的所有消息
        iDeepseekChatMessagesService.lambdaUpdate()
                .eq(DeepseekChatMessages::getSessionId, sessionId)
                .remove();
        // 删除会话
        iDeepseekChatSessionsService.removeById(sessionId);
    }

    @Override
    public Object getKey(UserInfo userInfo) {
        return iDeepseekChatMessagesService.getKey(userInfo.getId());
    }

    private static StringBuilder reasoningContent = new StringBuilder();
    private static StringBuilder finalContent = new StringBuilder();
    private static boolean isFirstPrint = true;
    private  void handleGenerationResult(GenerationResult message) {
        String reasoning = message.getOutput().getChoices().get(0).getMessage().getReasoningContent();
        String content = message.getOutput().getChoices().get(0).getMessage().getContent();
        if (reasoning != null && !reasoning.isEmpty()) {
            reasoningContent.append(reasoning);
            if (isFirstPrint) {
                System.out.println("====================思考过程====================");
                isFirstPrint = false;
            }
            System.out.print(reasoning);
        }
        if (content != null && !content.isEmpty()) {
            finalContent.append(content);
            if (!isFirstPrint) {
                System.out.println("\n====================完整回复====================");
                isFirstPrint = true;
            }
            System.out.print(content);
        }
    }
    private GenerationParam buildGenerationParam(Message userMsg) {
        return GenerationParam.builder()
                // 若没有配置环境变量，请用阿里云百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(token)
                .model("deepseek-v3.2")
                .enableThinking(true)
                .incrementalOutput(true)
                .resultFormat("message")
                .messages(Arrays.asList(userMsg))
                .build();
    }
    public   void streamCallWithMessage(Generation gen, Message userMsg)
            throws NoApiKeyException, ApiException, InputRequiredException {
        GenerationParam param = buildGenerationParam(userMsg);
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingForEach(message -> handleGenerationResult(message));
    }

    @Override
    public List<Object> getAgentTools() {
        return new ArrayList<>(AgentTools.getAllTools());
    }

    @Override
    public SseEmitter completionsAgentStream(UserInfo userInfo, SysChartCompletionRequestDTO dto) {
        log.info("[Agent] 开始处理Agent流式请求, sessionId={}, userId={}", dto.getSessionId(), userInfo.getId());
        SseEmitter emitter = new SseEmitter(600000L); // 10分钟超时

        // 在主线程获取用户可访问的菜单ID集合（避免异步线程中数据库上下文问题）
        final Set<Integer> allowedMenuIds = getUserAllowedMenuIds(userInfo);
        log.info("[Agent] 用户可访问菜单数: {}", allowedMenuIds != null ? allowedMenuIds.size() : "全部");

        // 捕获当前线程的Shiro上下文，传播到异步线程
        org.apache.shiro.mgt.SecurityManager securityManager = null;
        Subject subject = null;
        try {
            securityManager = SecurityUtils.getSecurityManager();
            subject = SecurityUtils.getSubject();
        } catch (Exception e) {
            log.warn("获取Shiro上下文失败，将不传播Shiro上下文: {}", e.getMessage());
        }

        // 保存最终值用于lambda表达式
        final org.apache.shiro.mgt.SecurityManager finalSecurityManager = securityManager;
        final Subject finalSubject = subject;

        log.info("[Agent] 提交异步任务到线程池");
        executorService.execute(() -> {
            log.info("[Agent] 异步任务开始执行");
            try {
                // 绑定Shiro上下文到异步线程
                if (finalSecurityManager != null) {
                    ThreadContext.bind(finalSecurityManager);
                }
                if (finalSubject != null) {
                    ThreadContext.bind(finalSubject);
                }
                // 立即发送"正在准备"状态事件，让用户知道后端已开始处理
                sendStatusEvent(emitter, "preparing", "正在准备中...");

                // 构建消息列表
                List<JSONObject> messages = new ArrayList<>();

                // 添加系统提示词，告诉AI它是一个ERP系统助手
                sendStatusEvent(emitter, "loading", "正在加载系统配置...");
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", buildAgentSystemPrompt(userInfo, allowedMenuIds, dto.getCurrentPage(), dto.getPageTitle(), dto.getHelpDocUrl(), dto.getHelpDocLibrary(), dto.getCurrentHelpDoc()));
                messages.add(systemMessage);

                // 获取历史消息（验证会话归属当前用户，防止跨租户访问）
                if (StrUtil.isNotBlank(dto.getSessionId())) {
                    sendStatusEvent(emitter, "loading", "正在加载历史消息...");
                    DeepseekChatSessions existingSession = iDeepseekChatSessionsService.lambdaQuery()
                            .eq(DeepseekChatSessions::getId, dto.getSessionId())
                            .eq(DeepseekChatSessions::getUserid, userInfo.getId())
                            .one();
                    if (existingSession == null) {
                        throw new RuntimeException("会话不存在或无权访问");
                    }
                    List<DeepseekChatMessages> historyMessages = iDeepseekChatMessagesService.lambdaQuery()
                            .eq(DeepseekChatMessages::getSessionId, dto.getSessionId())
                            .orderBy(true, true, DeepseekChatMessages::getCreatetime)
                            .list();
                    // 只保留最近的20条历史消息，避免历史消息过多导致AI依赖历史结果
                    int startIndex = Math.max(0, historyMessages.size() - 20);
                    for (int i = startIndex; i < historyMessages.size(); i++) {
                        DeepseekChatMessages message = historyMessages.get(i);
                        JSONObject msg = new JSONObject();
                        msg.put("role", message.getRole());
                        msg.put("content", message.getContent());
                        messages.add(msg);
                    }
                }

                // 添加新消息
                List<DeepseekChatMessages> newMessages = new ArrayList<>();
                for (DeepSeekMessage message : dto.getMessages()) {
                    DeepseekChatMessages e = new DeepseekChatMessages();
                    e.setSessionId(dto.getSessionId());
                    e.setCreatetime(new Date());
                    e.setContent(message.getContent());
                    e.setRole(message.getRole());
                    newMessages.add(e);

                    JSONObject msg = new JSONObject();
                    msg.put("role", message.getRole());
                    msg.put("content", message.getContent());
                    messages.add(msg);
                }

                // 获取工具定义
                sendStatusEvent(emitter, "loading", "正在加载工具定义...");
                List<JSONObject> tools = AgentTools.getAllTools();
                log.info("[Agent] 加载了{}个工具定义", tools.size());
                sendStatusEvent(emitter, "ready", String.format("已加载%d个工具，准备就绪", tools.size()));

                String sessionId = dto.getSessionId();
                int maxIterations = 15; // 最大循环次数，防止无限循环
                int iteration = 0;
                int toolCallRoundCount = 0; // 已执行工具调用的轮次计数
                StringBuilder fullContent = new StringBuilder();

                // 第一阶段：让AI选择需要使用的工具（按菜单权限过滤）
                sendStatusEvent(emitter, "selecting_tools", "正在分析问题，选择合适的工具...");
                List<JSONObject> toolSummaries = AgentTools.getToolSummaries(allowedMenuIds);
                List<String> selectedToolNames = selectToolsWithAI(messages, toolSummaries, dto.getModel());
                log.info("[Agent] AI选择了{}个工具: {}", selectedToolNames.size(), selectedToolNames);

                if (selectedToolNames.isEmpty()) {
                    // AI没有选择任何工具，直接告知用户没有相关权限
                    sendStatusEvent(emitter, "thinking", "AI正在思考中...");
                    String directContent;
                    // 检查用户是否在询问业务数据（关键词匹配）
                    String userQuestion = dto.getMessages() != null && !dto.getMessages().isEmpty() ?
                            dto.getMessages().get(dto.getMessages().size() - 1).getContent() : "";
                    if (containsBusinessKeyword(userQuestion)) {
                        directContent = "抱歉，您没有查询该数据的权限。如需开通相关功能，请联系管理员。";
                    } else {
                        JSONObject directResponse = callAIWithoutTools(messages, dto.getModel());
                        directContent = directResponse.getString("content");
                        if (StrUtil.isBlank(directContent)) {
                            directContent = "抱歉，我无法理解您的问题。请尝试重新描述您的问题。";
                        }
                    }
                    fullContent.append(directContent);
                    JSONObject contentEvent = new JSONObject();
                    contentEvent.put("content", directContent);
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .data(contentEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                } else {
                    // 第二阶段：使用提示词驱动的方式调用工具（不依赖function calling）
                    String toolPrompt = AgentTools.buildToolPromptForNames(selectedToolNames);
                    sendStatusEvent(emitter, "ready", String.format("已选择%d个工具，开始处理", selectedToolNames.size()));

                    // Agent循环
                    while (iteration < maxIterations) {
                        iteration++;
                        log.info("[Agent] 第{}轮循环开始", iteration);

                        // 发送"正在思考"状态事件
                        sendStatusEvent(emitter, "thinking",
                                iteration == 1 ? "AI正在思考中..." : "AI正在分析工具返回结果...");

                        // 构建带工具定义的提示词
                        List<JSONObject> agentMessages = new ArrayList<>();
                        // 复制原有消息
                        for (JSONObject msg : messages) {
                            agentMessages.add(msg);
                        }

                        // 每轮都添加工具调用指令（确保AI始终记得格式）
                        JSONObject toolInstruction = new JSONObject();
                        toolInstruction.put("role", "system");
                        if (iteration == 1) {
                            toolInstruction.put("content", buildToolCallInstruction(toolPrompt));
                        } else if (toolCallRoundCount >= 2) {
                            // 已经执行过多轮工具调用，强制要求给出最终答案
                            toolInstruction.put("content", "你已经调用了多次工具并获取了足够的数据。现在必须直接输出最终答案给用户，禁止再调用任何工具。用中文清晰地汇总所有已获取的数据，回答用户的问题。");
                        } else {
                            // 后续轮次使用简短提醒
                            toolInstruction.put("content", "重要提醒：\n" +
                                    "- 如果前面的工具调用已经返回了足够数据，你现在必须直接输出最终答案，禁止再调用工具\n" +
                                    "- 只有在缺少关键数据且必须补充时，才可以继续调用工具\n" +
                                    "- 绝对不要重复调用已经调用过的工具\n" +
                                    "- 输出最终答案时，用中文清晰地汇总数据\n" +
                                    "如需调用工具，只输出TOOL_CALL格式；如数据已足够，直接输出最终答案。");
                        }
                        agentMessages.add(toolInstruction);

                        // 使用function calling方式调用AI（支持所有OpenAI兼容模型：qwen/deepseek/kimi/minimax）
                        // 只传递AI选中的工具，而不是全部工具（避免token超限）
                        List<JSONObject> selectedTools = new ArrayList<>();
                        for (JSONObject tool : tools) {
                            JSONObject func = tool.getJSONObject("function");
                            String toolName = func != null ? func.getString("name") : null;
                            if (toolName != null && selectedToolNames.contains(toolName)) {
                                selectedTools.add(tool);
                            }
                        }
                        if (selectedTools.isEmpty()) {
                            log.warn("[Agent] 选中的工具列表为空，跳过本轮工具调用");
                            break;
                        }
                        int msgCount = agentMessages.size();
                        int toolCount = selectedTools.size();
                        log.info("[Agent] 调用AI接口, model={}, tools={}/{}", dto.getModel(), toolCount, tools.size());
                        log.info("[Agent] 调用AI接口: 消息数: {}, 工具数: {}", msgCount, toolCount);
                        sendAgentDetailEvent(emitter, "ai_calling", 
                            "第" + iteration + "轮: 调用AI模型 " + dto.getModel() + " (消息:" + msgCount + ", 工具:" + toolCount + ")");

                        JSONObject aiResponse = callAIWithTools(agentMessages, selectedTools, dto.getModel());
                        String aiContent = aiResponse.getString("content");
                        int respLen = aiContent != null ? aiContent.length() : 0;
                        boolean hasToolCalls = aiResponse.getJSONArray("tool_calls") != null && !aiResponse.getJSONArray("tool_calls").isEmpty();
                        log.info("[Agent] AI接口响应长度: {}", respLen);
                        log.info("[Agent] AI响应成功, hasToolCalls={}", hasToolCalls);
                        log.info("[Agent] AI响应长度: {}", respLen);
                        sendAgentDetailEvent(emitter, "ai_response", 
                            "第" + iteration + "轮: AI响应 " + (respLen > 0 ? "内容" + respLen + "字" : "无内容") + (hasToolCalls ? ", 包含工具调用" : ""));

                        if (aiResponse.containsKey("error")) {
                            String errorMsg = aiResponse.getString("error");
                            log.error("[Agent] AI调用失败: {}", errorMsg);
                            // 不直接break，而是将错误信息作为最终答案返回给用户
                            fullContent.append("AI调用失败: ").append(errorMsg);
                            JSONObject contentEvent = new JSONObject();
                            contentEvent.put("content", "AI调用失败: " + errorMsg);
                            emitter.send(SseEmitter.event()
                                    .id(String.valueOf(System.currentTimeMillis()))
                                    .data(contentEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                            break;
                        }

                        // 发送AI思考过程到前端（reasoning内容）
                        // kimi-k2-thinking模型使用reasoning_content字段
                        String reasoning = aiResponse.getString("reasoning_content");
                        if (StrUtil.isBlank(reasoning)) {
                            reasoning = aiResponse.getString("reasoning");
                        }
                        if (StrUtil.isNotBlank(reasoning)) {
                            JSONObject reasoningEvent = new JSONObject();
                            reasoningEvent.put("reasoning", reasoning);
                            emitter.send(SseEmitter.event()
                                    .id("reasoning-" + System.currentTimeMillis())
                                    .data(reasoningEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                        }

                        // 优先检查原生tool_calls（function calling格式）
                        JSONArray nativeToolCalls = aiResponse.getJSONArray("tool_calls");
                        List<JSONObject> parsedToolCalls = new ArrayList<>();

                        if (nativeToolCalls != null && !nativeToolCalls.isEmpty()) {
                            // 原生function calling响应，直接解析tool_calls
                            log.info("[Agent] 检测到原生tool_calls, 数量: {}", nativeToolCalls.size());
                            for (int i = 0; i < nativeToolCalls.size(); i++) {
                                JSONObject tc = nativeToolCalls.getJSONObject(i);
                                JSONObject function = tc.getJSONObject("function");
                                if (function != null) {
                                    JSONObject parsed = new JSONObject();
                                    parsed.put("tool", function.getString("name"));
                                    parsed.put("params", function.getString("arguments"));
                                    parsedToolCalls.add(parsed);
                                }
                            }
                        } else if (StrUtil.isNotBlank(aiContent)) {
                            // 没有原生tool_calls，尝试从文本中解析TOOL_CALL
                            // 检测占位符（如minimax:tool_call）
                            String trimmedContent = aiContent.trim();
                            if (trimmedContent.matches("^[a-zA-Z_-]+:tool_call$") || trimmedContent.contains(":tool_call")) {
                                log.warn("[Agent] 模型返回了tool_call占位符: '{}', iteration={}", trimmedContent, iteration);
                                continue;
                            }
                            parsedToolCalls = parseToolCallsFromText(aiContent);
                        }

                        // 如果既没有tool_calls，content也为空，则跳过本轮
                        if ((parsedToolCalls == null || parsedToolCalls.isEmpty()) && StrUtil.isBlank(aiContent)) {
                            log.warn("[Agent] AI返回空内容且无工具调用, iteration={}", iteration);
                            if (iteration >= maxIterations) {
                                fullContent.append("AI未能生成有效回复，请重试。");
                                JSONObject contentEvent = new JSONObject();
                                contentEvent.put("content", "AI未能生成有效回复，请重试。");
                                emitter.send(SseEmitter.event()
                                        .id(String.valueOf(System.currentTimeMillis()))
                                        .data(contentEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                            }
                            continue;
                        }

                        if (parsedToolCalls != null && !parsedToolCalls.isEmpty()) {
                            // 有工具调用
                            log.info("[Agent] 解析到{}个工具调用", parsedToolCalls.size());

                            // 提取思考内容（工具调用标记之前的部分）
                            String thinkingContent = extractThinkingContent(aiContent);
                            if (StrUtil.isNotBlank(thinkingContent)) {
                                JSONObject reasoningEvent = new JSONObject();
                                reasoningEvent.put("reasoning", thinkingContent);
                                emitter.send(SseEmitter.event()
                                        .id(String.valueOf(System.currentTimeMillis()))
                                        .data(reasoningEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                            }

                            // 构建工具调用摘要（不含TOOL_CALL原文，避免混淆AI）
                            StringBuilder toolCallSummary = new StringBuilder();
                            for (JSONObject tc : parsedToolCalls) {
                                toolCallSummary.append("调用工具: ").append(tc.getString("tool")).append("\n");
                            }

                            // 添加AI回复到消息列表（支持原生tool_calls格式）
                            JSONObject assistantMessage = new JSONObject();
                            assistantMessage.put("role", "assistant");
                            if (nativeToolCalls != null && !nativeToolCalls.isEmpty()) {
                                // 原生function calling：保留content和tool_calls
                                assistantMessage.put("content", StrUtil.isNotBlank(aiContent) ? aiContent : "");
                                assistantMessage.put("tool_calls", nativeToolCalls);
                            } else {
                                // 文本解析模式：只保留思考内容
                                String cleanContent = StrUtil.isNotBlank(thinkingContent) ? thinkingContent : toolCallSummary.toString();
                                assistantMessage.put("content", cleanContent);
                            }
                            messages.add(assistantMessage);

                            // 构建已授权工具名称集合，用于二次校验
                            Set<String> allowedToolNameSet = new HashSet<>(selectedToolNames);

                            // 执行所有工具调用（AI可能一次输出多个，只要参数是具体的就全部执行）
                            for (int tcIdx = 0; tcIdx < parsedToolCalls.size(); tcIdx++) {
                                JSONObject toolCall = parsedToolCalls.get(tcIdx);
                                String functionName = toolCall.getString("tool");
                                String functionArgs = toolCall.getString("params");

                                // 权限二次校验：确保工具在第一阶段筛选通过的列表中
                                if (!allowedToolNameSet.contains(functionName)) {
                                    log.warn("[Agent] 工具不在授权列表中，拒绝执行: {}", functionName);
                                    JSONObject denyEvent = new JSONObject();
                                    denyEvent.put("type", "tool_result");
                                    denyEvent.put("function_name", functionName);
                                    denyEvent.put("result", "{\"success\":false,\"error\":\"权限不足：您没有使用该工具的权限。请联系管理员开通相应权限。\"}");
                                    emitter.send(SseEmitter.event()
                                            .id("tool_result_" + System.currentTimeMillis())
                                            .data(denyEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                                    continue;
                                }

                                // 获取原生tool_call_id（用于function calling格式）
                                String toolCallId = null;
                                if (nativeToolCalls != null && tcIdx < nativeToolCalls.size()) {
                                    toolCallId = nativeToolCalls.getJSONObject(tcIdx).getString("id");
                                }

                                // 获取工具详细信息
                                JSONObject toolInfo = AgentTools.getToolInfo(functionName);

                                // 发送工具调用事件
                                log.info("[Agent] AI请求调用工具: {}, args={}", functionName, functionArgs);
                                JSONObject toolCallEvent = new JSONObject();
                                toolCallEvent.put("type", "tool_call");
                                toolCallEvent.put("function_name", functionName);
                                if (toolInfo != null) {
                                    toolCallEvent.put("function_description", toolInfo.getString("description"));
                                    toolCallEvent.put("endpoint", toolInfo.getString("endpoint"));
                                    toolCallEvent.put("method", toolInfo.getString("method"));
                                    toolCallEvent.put("service", toolInfo.getString("service"));
                                    toolCallEvent.put("module", toolInfo.getString("module"));
                                }
                                toolCallEvent.put("arguments", functionArgs);
                                emitter.send(SseEmitter.event()
                                        .id("tool_call_" + System.currentTimeMillis())
                                        .data(toolCallEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));

                                // 执行工具（带错误处理）
                                String toolDisplayName = toolInfo != null ? toolInfo.getString("description") : functionName;
                                if (toolDisplayName != null && toolDisplayName.length() > 20) {
                                    toolDisplayName = toolDisplayName.substring(0, 20) + "...";
                                }
                                sendStatusEvent(emitter, "executing_tool", "正在调用工具: " + toolDisplayName);
                                String toolResult;
                                try {
                                    toolResult = agentToolExecutor.execute(functionName, functionArgs, userInfo);
                                } catch (Exception toolEx) {
                                    String errorMsg = toolEx.getMessage();
                                    if (errorMsg != null && errorMsg.contains("权限")) {
                                        toolResult = "{\"success\":false,\"error\":\"权限不足：" + errorMsg + "。请联系管理员开通相应权限。\"}";
                                    } else {
                                        toolResult = "{\"success\":false,\"error\":\"工具执行失败：" + errorMsg + "\"}";
                                    }
                                    log.error("[Agent] 工具执行异常: {}", functionName, toolEx);
                                }
                                log.info("[Agent] 工具执行完成: {}, 结果长度: {}", functionName, toolResult != null ? toolResult.length() : 0);
                                sendStatusEvent(emitter, "tool_done", "工具调用完成");

                                // 发送工具结果事件
                                JSONObject toolResultEvent = new JSONObject();
                                toolResultEvent.put("type", "tool_result");
                                toolResultEvent.put("function_name", functionName);
                                if (toolInfo != null) {
                                    toolResultEvent.put("endpoint", toolInfo.getString("endpoint"));
                                }
                                toolResultEvent.put("result", toolResult);
                                emitter.send(SseEmitter.event()
                                        .id("tool_result_" + System.currentTimeMillis())
                                        .data(toolResultEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));

                                // 将工具结果添加到消息列表
                                JSONObject toolMessage = new JSONObject();
                                if (toolCallId != null) {
                                    // 原生function calling格式：使用tool角色
                                    toolMessage.put("role", "tool");
                                    toolMessage.put("tool_call_id", toolCallId);
                                    toolMessage.put("content", toolResult);
                                } else {
                                    // 文本解析格式：使用user角色模拟
                                    toolMessage.put("role", "user");
                                    toolMessage.put("content", "以下是工具 [" + functionName + "] 的返回结果，请根据此数据继续执行下一步或给出最终答案：\n" + toolResult);
                                }
                                messages.add(toolMessage);
                            }

                            // 增加工具调用轮次计数
                            toolCallRoundCount++;

                            // 添加工具结果汇总消息，帮助AI理解已完成的工作
                            JSONObject summaryMsg = new JSONObject();
                            summaryMsg.put("role", "system");
                            summaryMsg.put("content", "本轮已执行 " + parsedToolCalls.size() + " 个工具调用，结果已在上方。" +
                                    "累计已执行 " + toolCallRoundCount + " 轮工具调用。" +
                                    "请根据已获取的所有数据直接输出最终答案给用户，不要再调用工具。");
                            messages.add(summaryMsg);

                            // 继续循环，让AI处理工具结果
                            log.info("[Agent] 工具执行完毕，已执行{}轮工具调用，继续下一轮循环，当前消息数: {}", toolCallRoundCount, messages.size());
                            continue;

                        } else {
                            // 没有工具调用，检测是否为过渡性回复（如"请稍等，我将调用工具"）
                            boolean isIntermediate = isIntermediateResponse(aiContent);
                            if (isIntermediate && iteration < maxIterations - 2) {
                                // 过渡性回复：不输出给用户，强制AI继续处理
                                log.info("[Agent] 检测到过渡性回复，强制AI继续处理: {}", aiContent.substring(0, Math.min(aiContent.length(), 50)));
                                sendAgentDetailEvent(emitter, "ai_response",
                                    "第" + iteration + "轮: AI返回了过渡性回复，正在引导AI继续处理...");
                                // 添加用户消息，要求AI立即执行操作而非回复过渡性文字
                                JSONObject continueMsg = new JSONObject();
                                continueMsg.put("role", "user");
                                continueMsg.put("content", "不要回复过渡性文字。请立即执行工具调用或给出最终数据结果。如果需要调用工具，直接输出TOOL_CALL；如果数据已足够，直接用中文汇总数据回答。");
                                messages.add(continueMsg);
                                continue;
                            }
                            // 真正的最终答案
                            fullContent.append(aiContent);
                            JSONObject contentEvent = new JSONObject();
                            contentEvent.put("content", aiContent);
                            emitter.send(SseEmitter.event()
                                    .id(String.valueOf(System.currentTimeMillis()))
                                    .data(contentEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                            break;
                        }
                    } // end while

                    // 如果循环结束后仍未生成最终答案（如模型一直返回工具调用），尝试让AI给出最终答案
                    if (fullContent.length() == 0) {
                        log.warn("[Agent] 循环结束未生成最终答案，已执行{}轮工具调用，尝试最后一次强制回答", toolCallRoundCount);
                        // 最后一次尝试：强制AI给出最终答案（不带工具）
                        try {
                            JSONObject lastMsg = new JSONObject();
                            lastMsg.put("role", "user");
                            lastMsg.put("content", "请根据上面所有的工具返回结果，直接用中文汇总回答用户的问题。不要调用任何工具。");
                            messages.add(lastMsg);
                            JSONObject lastResponse = callAIWithoutTools(messages, dto.getModel());
                            String lastContent = lastResponse.getString("content");
                            if (StrUtil.isNotBlank(lastContent)) {
                                fullContent.append(lastContent);
                                JSONObject contentEvent = new JSONObject();
                                contentEvent.put("content", lastContent);
                                emitter.send(SseEmitter.event()
                                        .id(String.valueOf(System.currentTimeMillis()))
                                        .data(contentEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                            }
                        } catch (Exception lastEx) {
                            log.error("[Agent] 最后一次尝试也失败了", lastEx);
                        }
                        // 如果仍然没有内容，使用普通AI直接回答原始问题
                        if (fullContent.length() == 0) {
                            log.warn("[Agent] 工具调用后未生成回复，使用普通AI直接回答原始问题");
                            try {
                                // 构建原始问题消息（不带工具上下文）
                                List<JSONObject> simpleMessages = new ArrayList<>();
                                // 只保留用户的第一条消息
                                for (JSONObject msg : messages) {
                                    if ("user".equals(msg.getString("role"))) {
                                        simpleMessages.add(msg);
                                        break;
                                    }
                                }
                                if (!simpleMessages.isEmpty()) {
                                    JSONObject simpleResponse = callAIWithoutTools(simpleMessages, dto.getModel());
                                    String simpleContent = simpleResponse.getString("content");
                                    if (StrUtil.isNotBlank(simpleContent)) {
                                        fullContent.append(simpleContent);
                                        JSONObject contentEvent = new JSONObject();
                                        contentEvent.put("content", simpleContent);
                                        emitter.send(SseEmitter.event()
                                                .id(String.valueOf(System.currentTimeMillis()))
                                                .data(contentEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                                    }
                                }
                            } catch (Exception simpleEx) {
                                log.error("[Agent] 普通AI回答也失败", simpleEx);
                            }
                        }
                        // 如果仍然没有内容，显示错误信息
                        if (fullContent.length() == 0) {
                            String fallbackMsg = "AI在" + toolCallRoundCount + "轮工具调用后未能生成最终回复，请尝试重新提问或简化问题。";
                            fullContent.append(fallbackMsg);
                            JSONObject contentEvent = new JSONObject();
                            contentEvent.put("content", fallbackMsg);
                            emitter.send(SseEmitter.event()
                                    .id(String.valueOf(System.currentTimeMillis()))
                                    .data(contentEvent.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                        }
                    }
                } // end else

                // 保存会话和消息
                DeepseekChatSessions session = null;
                if (StrUtil.isBlankOrUndefined(sessionId)) {
                    sessionId = UUID.randomUUID().toString();
                    for (DeepseekChatMessages message : newMessages) {
                        message.setSessionId(sessionId);
                    }
                    session = new DeepseekChatSessions();
                    session.setId(sessionId);
                    session.setUserid(userInfo.getId());
                    session.setOpttime(new Date());
                    session.setModel(dto.getModel());
                    session.setCreatetime(new Date());
                    session.setTitle(extractSessionTitle(dto.getMessages()));
                    this.iDeepseekChatSessionsService.save(session);
                } else {
                    session = iDeepseekChatSessionsService.getById(sessionId);
                }

                // 保存AI回复消息
                DeepseekChatMessages aiMessage = new DeepseekChatMessages();
                aiMessage.setSessionId(sessionId);
                aiMessage.setCreatetime(new Date());
                aiMessage.setContent(fullContent.toString());
                aiMessage.setRole("assistant");
                newMessages.add(aiMessage);
                iDeepseekChatMessagesService.saveBatch(newMessages);

                // 【P1】对话结束后异步执行学习分析
                final String finalSessionId = sessionId;
                final String userMsg = dto.getMessages() != null && !dto.getMessages().isEmpty()
                        ? dto.getMessages().get(dto.getMessages().size() - 1).getContent() : "";
                final String aiReply = fullContent.toString();
                executorService.execute(() -> {
                    try {
                        analyzeAndRecordLearning(userMsg, aiReply, finalSessionId, userInfo);
                    } catch (Exception ex) {
                        log.warn("学习分析失败（不影响正常使用）: {}", ex.getMessage());
                    }
                });

                // 发送完成事件
                JSONObject completeData = new JSONObject();
                completeData.put("id", sessionId);
                completeData.put("content", "[DONE]");
                log.info("[Agent] Agent流式完成, sessionId={}", sessionId);
                emitter.send(SseEmitter.event()
                        .id("complete")
                        .data(completeData.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));

                emitter.complete();
                log.info("[Agent] SSE连接已关闭");

            } catch (Exception e) {
                log.error("[Agent] Agent流式请求失败", e);
                try {
                    JSONObject errorData = new JSONObject();
                    errorData.put("error", e.getMessage());
                    emitter.send(SseEmitter.event()
                            .data(errorData.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
                } catch (IOException ex) {
                    log.error("发送错误信息失败", ex);
                }
                emitter.completeWithError(e);
            } finally {
                // 清理Shiro上下文，防止内存泄漏
                ThreadContext.unbindSecurityManager();
                ThreadContext.unbindSubject();
            }
        });

        emitter.onTimeout(() -> {
            log.warn("Agent SSE连接超时");
            emitter.complete();
        });

        emitter.onError(e -> {
            log.error("Agent SSE连接错误", e);
        });

        return emitter;
    }

    /**
     * 从消息列表中提取会话标题：取最后一条用户消息，截取前50个字符
     */
    private String extractSessionTitle(List<DeepSeekMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "新会话";
        }
        // 从后往前找第一条用户消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            DeepSeekMessage msg = messages.get(i);
            if ("user".equals(msg.getRole()) && msg.getContent() != null && !msg.getContent().trim().isEmpty()) {
                String title = msg.getContent().trim().replaceAll("\\s+", " ");
                return title.length() > 50 ? title.substring(0, 50) + "..." : title;
            }
        }
        return "新会话";
    }

    /**
     * 判断用户问题中是否包含业务查询关键词
     */
    private boolean containsBusinessKeyword(String question) {
        if (StrUtil.isBlank(question)) {
            return false;
        }
        String[] keywords = {"店铺", "订单", "库存", "采购", "商品", "物流", "货件", "财务", "销售",
                "广告", "FBA", "listing", "SKU", "仓库", "盘点", "调拨", "退货", "结算",
                "查询", "查看", "多少", "哪些", "列表", "报表"};
        for (String keyword : keywords) {
            if (question.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建Agent系统提示词
     */
    private String buildAgentSystemPrompt(UserInfo userInfo) {
        return buildAgentSystemPrompt(userInfo, null, null, null, null, null);
    }

    private String buildAgentSystemPrompt(UserInfo userInfo, Set<Integer> allowedMenuIds) {
        return buildAgentSystemPrompt(userInfo, allowedMenuIds, null, null, null, null);
    }

    private String buildAgentSystemPrompt(UserInfo userInfo, Set<Integer> allowedMenuIds, String currentPage) {
        return buildAgentSystemPrompt(userInfo, allowedMenuIds, currentPage, null, null, null);
    }

    private String buildAgentSystemPrompt(UserInfo userInfo, Set<Integer> allowedMenuIds, String currentPage, String pageTitle, String helpDocUrl) {
        return buildAgentSystemPrompt(userInfo, allowedMenuIds, currentPage, pageTitle, helpDocUrl, null);
    }

    private String buildAgentSystemPrompt(UserInfo userInfo, Set<Integer> allowedMenuIds, String currentPage, String pageTitle, String helpDocUrl, String helpDocLibrary) {
        return buildAgentSystemPrompt(userInfo, allowedMenuIds, currentPage, pageTitle, helpDocUrl, helpDocLibrary, null);
    }

    private String buildAgentSystemPrompt(UserInfo userInfo, Set<Integer> allowedMenuIds, String currentPage, String pageTitle, String helpDocUrl, String helpDocLibrary, String currentHelpDoc) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是wimoor ERP系统的AI智能助手，专门帮助用户查询和分析仓储管理、订单、采购、库存、财务等业务数据。\n\n");
        // 注入当前日期，避免AI使用训练数据中的旧日期
        sb.append("当前日期：").append(new java.text.SimpleDateFormat("yyyy年MM月dd日").format(new java.util.Date())).append("\n");
        sb.append("当用户说\"这个月\"时，指的是当前月份；说\"今天\"时，指的是当前日期。请使用准确的日期参数。\n\n");

        // 注入当前页面上下文
        if (StrUtil.isNotBlank(currentPage)) {
            sb.append("当前用户所在页面路径：").append(currentPage).append("\n");
        }
        if (StrUtil.isNotBlank(pageTitle)) {
            sb.append("当前页面名称：").append(pageTitle).append("\n");
        }
        if (StrUtil.isNotBlank(helpDocUrl)) {
            sb.append("当前页面帮助文档链接：").append(helpDocUrl).append("\n");
        }
        if (StrUtil.isNotBlank(currentPage) || StrUtil.isNotBlank(pageTitle)) {
            sb.append("请根据当前页面上下文，主动提供该页面相关的功能说明和操作指导。\n\n");
        }

        // 注入当前页面的帮助文档内容
        boolean ignoreHelpDoc = StrUtil.isBlank(helpDocUrl) && StrUtil.isBlank(helpDocLibrary) && StrUtil.isBlank(currentHelpDoc);
        if (StrUtil.isNotBlank(currentHelpDoc) && !ignoreHelpDoc) {
            sb.append("## 当前页面的帮助文档内容\n");
            sb.append("以下是当前页面对应的功能模块帮助文档，请基于此文档内容回答用户关于当前页面的问题：\n");
            sb.append(currentHelpDoc).append("\n\n");
            sb.append("使用说明：\n");
            sb.append("1. 当用户询问当前页面如何操作时，直接基于上述文档内容给出详细的操作步骤\n");
            sb.append("2. 不要让用户去看文档，要直接指导用户完成操作\n");
            sb.append("3. 如果用户的问题超出当前页面范围，可以调用帮助文档工具查询其他文档\n\n");
        }

        // 注入整个帮助文档库
        if (StrUtil.isNotBlank(helpDocLibrary) && !ignoreHelpDoc) {
            sb.append("## 系统帮助文档库索引\n");
            sb.append("以下是系统所有功能模块的帮助文档索引，当用户问题涉及其他页面时，可以调用helpdoc_search或helpdoc_get工具获取相关文档：\n");
            sb.append(helpDocLibrary).append("\n\n");
        }

        sb.append("你的能力：\n");
        sb.append("1. 可以查询商品库存信息，包括各仓库的库存分布\n");
        sb.append("2. 可以查询订单信息，包括订单状态、详情、退货等\n");
        sb.append("3. 可以查询采购单信息，包括采购进度、付款情况等\n");
        sb.append("4. 可以查询商品信息，包括SKU、成本价、售价等\n");
        sb.append("5. 可以查询货件/物流信息，包括发货计划、追踪信息等\n");
        sb.append("6. 可以查询仓库信息\n");
        sb.append("7. 可以查询财务数据，包括销售额、利润、结算报告等\n");
        sb.append("8. 可以查询广告投放效果\n");
        sb.append("9. 可以查询FBA库存和费用\n");
        sb.append("10. 可以分析销售趋势和排行榜\n\n");
        sb.append("使用规则：\n");
        sb.append("- 当用户询问业务数据时，主动调用相关工具获取真实数据\n");
        sb.append("- 根据工具返回的数据，用清晰、专业的方式回答用户\n");
        sb.append("- 如果工具返回的数据为空或服务不可用，如实告知用户\n");
        sb.append("- 对于复杂问题，可以调用多个工具获取数据后综合分析\n");
        sb.append("- 回答要简洁明了，重点突出，必要时使用表格或列表展示数据\n");
        sb.append("- 使用中文回答\n");
        sb.append("- 工具返回数据为空时，检查参数是否正确（特别是isDelete、search等），不要编造数据\n");
        sb.append("- **重要**：只能调用上面\"可用工具\"中列出的工具。如果用户询问的功能不在可用工具列表中，直接告知用户：\"抱歉，您没有该功能的权限，如需开通请联系管理员。\"\n");
        sb.append("- **禁止**：不要输出任何工具调用代码（如 amz_xxx()），如果该工具不在可用工具列表中\n");
        if (!ignoreHelpDoc) {
            sb.append("\n帮助文档工具使用规则（非常重要！必须严格遵守）：\n");
            sb.append("当用户询问\"如何使用\"、\"怎么操作\"、\"在哪里\"、\"功能说明\"、\"操作指南\"等系统使用问题时，你必须：\n");
            sb.append("1. **首先调用 helpdoc_search 工具**，使用用户问题中的关键词搜索相关帮助文档\n");
            sb.append("2. **阅读工具返回的文档内容**，基于文档内容给出详细的、分步骤的操作指导\n");
            sb.append("3. 如果搜索结果不够详细，可以调用 helpdoc_get 工具获取特定文档的完整内容\n");
            sb.append("4. 如果涉及多个功能模块，可以多次调用工具获取多个文档，给出综合性的指导\n");
            sb.append("5. **不要只给出文档链接**，要基于文档内容直接回答用户的问题\n\n");
            sb.append("回复格式要求：\n");
            sb.append("「关于XX功能，以下是详细的操作步骤：\n\n");
            sb.append("**第一步：进入功能页面**\n");
            sb.append("菜单路径：XXX > XXX > XXX\n\n");
            sb.append("**第二步：执行操作**\n");
            sb.append("1. 点击[按钮名称]\n");
            sb.append("2. 填写[必填字段]\n");
            sb.append("3. ...\n\n");
            sb.append("**注意事项：**\n");
            sb.append("- [注意事项1]\n");
            sb.append("- [注意事项2]\n\n");
            sb.append("**相关功能：**\n");
            sb.append("如果您还需要XX功能，可以参考：[相关文档]」\n\n");
            sb.append("注意区分：用户询问\"业务数据\"（如查库存、查订单）时使用数据查询工具；用户询问\"如何操作\"时使用帮助文档工具\n");
        }
        sb.append("\n自动链式调用规则（非常重要！必须严格遵守）：\n");
        sb.append("- 当需要查询数据时，必须使用系统提供的function calling功能调用工具\n");
        sb.append("- 不要输出TOOL_CALL文本格式，直接调用函数\n");
        sb.append("- 如果参数已确定，可以一次调用多个函数\n");
        sb.append("- 如果参数依赖上一步结果，先调用一个函数，等收到结果后再调用下一个\n");
        sb.append("- 收到工具结果后，自动执行下一步，不要停下来等用户确认\n");
        sb.append("- 只有在获取到所有数据后，才输出最终答案\n");
        sb.append("- 如果工具调用失败，自动修正参数重试，不要让用户手动确认\n");
        sb.append("\n工具选择规则（非常重要！必须严格遵守）：\n");
        sb.append("- 查询ERP物料/产品/商品信息 → 必须使用 material_list，不要使用 amz_ 开头的工具\n");
        sb.append("- 查询ERP库存 → 必须使用 inv_list 或 inv_list_turnover，不要使用 amz_query_fba_inventory\n");
        sb.append("- 查询采购单 → 必须使用 purchase_list，不要使用 amz_ 开头的工具\n");
        sb.append("- 查询Amazon订单/FBA库存/广告/结算 → 使用 amz_ 开头的工具\n");
        sb.append("- 物料查询时，必须传 isDelete=0 查询正常物料\n");
        sb.append("- 查询\"店铺销售情况\"或\"店铺销量\"→ 必须使用 amz_query_order_summary 或 amz_query_order_total（订单维度汇总）\n");
        sb.append("- 查询\"产品销售情况\"或\"某个SKU卖了多少\"→ 使用 amz_query_product_list 或具体产品工具（产品维度）\n");
        sb.append("- 注意区分\"店铺销售\"和\"产品销售\"：店铺销售是订单汇总数据，产品销售是具体SKU数据\n");
        sb.append("\n参数传递规则（非常重要！必须严格遵守）：\n");
        sb.append("- 查询库存时，必须传递search参数，值为用户提供的SKU或产品名称，如 {\"search\": \"44\", \"islike\": true}\n");
        sb.append("- 查询库存时，如果用户指定了仓库（如\"龙华仓-正品仓\"），必须先调用warehouse_list获取仓库ID，再传递warehouseid参数\n");
        sb.append("- 查询库存时，如果用户没有指定仓库，不要传递warehouseid参数，会返回所有仓库的库存汇总\n");
        sb.append("- 查询物料时，search参数传用户提供的关键词，如用户说\"产品名称：手机\"，则传 {\"search\": \"手机\", \"searchtype\": \"name\", \"isDelete\": 0}\n");
        sb.append("- 查询物料时按SKU搜索，必须传 {\"search\": \"SKU值\", \"searchtype\": \"sku\", \"isDelete\": 0}\n");
        sb.append("- 不要对参数值做任何修改或转换，直接传递用户提供的原始值\n");
        sb.append("- 如果查询结果为空，告知用户未找到数据，建议检查SKU或产品名称是否正确\n");
        sb.append("- 库存查询返回结果中：fulfillable=指定仓库可用库存，allfulfillable=所有仓库可用库存汇总\n");
        sb.append("- 每次用户询问数据时，都必须重新调用工具查询，不要依赖历史消息中的查询结果\n");

        // 注入工具描述和上下文（按权限过滤）
        sb.append(AgentTools.getToolDescription(allowedMenuIds));

        // 注入学习知识（从t_sys_agent_learning加载）
        try {
            IToolAgentLearningService learningService = SpringUtil.getBean(IToolAgentLearningService.class);
            if (userInfo != null && userInfo.getCompanyid() != null) {
                Long shopid = Long.parseLong(userInfo.getCompanyid());
                Long userid = userInfo.getId() != null ? Long.parseLong(userInfo.getId()) : null;
                String learningPrompt = learningService.buildLearningPrompt(shopid, userid, 2000);
                if (learningPrompt != null && !learningPrompt.isEmpty()) {
                    sb.append(learningPrompt);
                    // 异步记录命中（被注入到系统提示词即视为命中）
                    executorService.execute(() -> {
                        try {
                            List<ToolAgentLearning> activeLearnings = learningService.getActiveLearnings(shopid, userid);
                            if (activeLearnings != null) {
                                for (ToolAgentLearning l : activeLearnings) {
                                    learningService.recordHit(l.getId());
                                }
                            }
                        } catch (Exception ignored) {}
                    });
                }
            }
        } catch (Exception e) {
            // 学习知识加载失败不影响正常使用
        }

        return sb.toString();
    }

    /**
     * 第一阶段：让AI分析问题并选择需要使用的工具
     * 1. 先让AI理解用户的问题意图
     * 2. 根据问题意图选择合适的工具
     * 返回AI选择的工具名称列表
     */
    private List<String> selectToolsWithAI(List<JSONObject> messages, List<JSONObject> toolSummaries, String model) {
        List<String> selectedTools = new ArrayList<>();
        try {
            // 获取用户最后一条消息
            String userQuestion = messages.size() > 1 ? messages.get(messages.size() - 1).getString("content") : "";

            // 构建工具选择的提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是ERP系统的问题分析助手。请按以下步骤处理用户问题：\n\n");
            prompt.append("## 第一步：分析问题\n");
            prompt.append("请分析用户的问题，明确：\n");
            prompt.append("1. 用户想要查询什么数据？\n");
            prompt.append("2. 涉及哪个业务模块？（订单/库存/采购/商品/财务等）\n");
            prompt.append("3. 有什么筛选条件？\n\n");

            prompt.append("## 第二步：选择工具\n");
            prompt.append("根据问题分析结果，从以下工具列表中选择需要使用的工具：\n\n");

            // 构建工具列表
            for (JSONObject summary : toolSummaries) {
                prompt.append("- ").append(summary.getString("name"))
                        .append("：").append(summary.getString("description")).append("\n");
            }

            prompt.append("\n## 输出格式\n");
            prompt.append("请严格按照以下格式输出，不要输出其他内容：\n");
            prompt.append("问题分析：[简要分析用户问题]\n");
            prompt.append("选择工具：[工具名称1,工具名称2]\n\n");
            prompt.append("如果不需要使用任何工具，工具部分写：选择工具：无\n");

            // 构建消息
            List<JSONObject> selectMessages = new ArrayList<>();
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", prompt.toString());
            selectMessages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", "用户问题：" + userQuestion);
            selectMessages.add(userMsg);

            // 调用AI
            JSONObject response = callAIWithoutTools(selectMessages, model);
            String content = response.getString("content");

            if (StrUtil.isNotBlank(content)) {
                log.info("[Agent] 工具选择AI返回: {}", content);

                // 解析问题分析和工具选择
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (line.startsWith("选择工具：") || line.startsWith("选择工具:")) {
                        String toolPart = line.substring("选择工具：".length()).trim();
                        if (!"无".equals(toolPart) && !toolPart.isEmpty()) {
                            // 移除方括号
                            toolPart = toolPart.replace("[", "").replace("]", "");
                            // 解析工具名称
                            toolPart = toolPart.replace("，", ",").replace("、", ",");
                            String[] toolNames = toolPart.split(",");
                            for (String name : toolNames) {
                                name = name.trim();
                                if (!name.isEmpty()) {
                                    selectedTools.add(name);
                                }
                            }
                        }
                        break;
                    }
                }
            }

            log.info("[Agent] 工具选择结果: {}", selectedTools);
        } catch (Exception e) {
            log.error("[Agent] 工具选择失败", e);
        }
        return selectedTools;
    }

    /**
     * 调用AI（不使用工具）
     * 用于第一阶段工具选择和不需要工具的直接回答
     */
    private JSONObject callAIWithoutTools(List<JSONObject> messages, String model) {
        JSONObject errorResult = new JSONObject();
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model != null ? model : "qwen-turbo");
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);

            MediaType mediaType = MediaType.parse("application/json");
            String requestJson = requestBody.toJSONString();
            log.info("[Agent] AI请求体大小: {} 字符, 消息数: {}, 模型: {}", requestJson.length(), messages.size(), requestBody.getString("model"));
            RequestBody body = RequestBody.create(mediaType, requestJson);

            String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            OkHttpClient client = getClient();
            Response response = client.newCall(request).execute();

            if (response == null || response.body() == null) {
                errorResult.put("error", "AI服务无响应");
                return errorResult;
            }

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                log.error("[Agent] AI服务返回HTTP {}, 响应体: {}", response.code(), errorBody);
                errorResult.put("error", "AI服务返回HTTP " + response.code() + ": " + errorBody);
                return errorResult;
            }

            String result = response.body().string();
            JSONObject resultJson = JSONObject.parseObject(result);

            if (resultJson.containsKey("error")) {
                errorResult.put("error", "AI服务返回错误");
                return errorResult;
            }

            JSONArray choices = resultJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    String content = message.getString("content");
                    JSONArray toolCalls = message.getJSONArray("tool_calls");
                    // 如果有原生tool_calls，将其转换为文本格式的TOOL_CALL
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        if (StrUtil.isNotBlank(content)) {
                            sb.append(content).append("\n");
                        }
                        for (int i = 0; i < toolCalls.size(); i++) {
                            JSONObject tc = toolCalls.getJSONObject(i);
                            JSONObject function = tc.getJSONObject("function");
                            if (function != null) {
                                String funcName = function.getString("name");
                                String funcArgs = function.getString("arguments");
                                sb.append("TOOL_CALL\n{\"tool\":\"").append(funcName).append("\",\"params\":").append(funcArgs).append("}\n");
                            }
                        }
                        message.put("content", sb.toString());
                        log.info("[Agent] 原生tool_calls已转换为文本格式, 数量: {}", toolCalls.size());
                    }
                    if (StrUtil.isBlank(message.getString("content"))) {
                        log.warn("[Agent] AI返回了空content, 完整响应: {}", result);
                    }
                    return message;
                }
            }

            log.warn("[Agent] AI返回空choices, 完整响应: {}", result);
            errorResult.put("error", "AI服务返回空响应: " + result);
            return errorResult;
        } catch (Exception e) {
            log.error("[Agent] AI调用失败", e);
            errorResult.put("error", "AI调用失败: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * 调用AI（支持工具调用）
     * 使用OkHttp调用DashScope OpenAI兼容接口
     * 返回包含AI响应的JSONObject，失败时返回包含error字段的结果
     */
    private JSONObject callAIWithTools(List<JSONObject> messages, List<JSONObject> tools, String model) {
        JSONObject errorResult = new JSONObject();
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model != null ? model : "qwen-turbo");
            requestBody.put("messages", messages);
            requestBody.put("tools", tools);
            requestBody.put("tool_choice", "auto");
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4096);

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestBody.toJSONString());

            String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
            log.info("[Agent] 调用AI接口: {}, 消息数: {}, 工具数: {}", apiUrl, messages.size(), tools.size());
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            OkHttpClient client = getClient();
            Response response = client.newCall(request).execute();

            if (response == null || response.body() == null) {
                String err = "AI服务无响应（response is null）";
                log.error("[Agent调用失败] {}", err);
                errorResult.put("error", err);
                errorResult.put("detail", "API地址: " + apiUrl + "，请检查网络连接或AI服务是否可用");
                return errorResult;
            }

            if (!response.isSuccessful()) {
                String responseBody = response.body().string();
                String err = "AI服务返回HTTP " + response.code();
                log.error("[Agent调用失败] {} 响应体: {}", err, responseBody);

                // 尝试解析DashScope的错误信息
                String detail = responseBody;
                try {
                    JSONObject errJson = JSONObject.parseObject(responseBody);
                    if (errJson.containsKey("error")) {
                        JSONObject errObj = errJson.getJSONObject("error");
                        detail = errObj.getString("message");
                        if (errObj.containsKey("code")) {
                            detail = "[" + errObj.getString("code") + "] " + detail;
                        }
                    }
                } catch (Exception ignored) {}

                errorResult.put("error", err);
                errorResult.put("detail", detail);
                return errorResult;
            }

            String result = response.body().string();
            log.info("[Agent] AI接口响应长度: {}", result.length());
            log.debug("[Agent] AI接口原始响应: {}", result);
            JSONObject resultJson = JSONObject.parseObject(result);

            // 检查API是否返回了错误
            if (resultJson.containsKey("error")) {
                JSONObject errObj = resultJson.getJSONObject("error");
                String err = "AI服务返回错误";
                String detail = errObj.getString("message");
                if (errObj.containsKey("code")) {
                    detail = "[" + errObj.getString("code") + "] " + detail;
                }
                log.error("[Agent调用失败] {} - {}", err, detail);
                errorResult.put("error", err);
                errorResult.put("detail", detail);
                return errorResult;
            }

            // 解析响应
            JSONArray choices = resultJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                if (message != null) {
                    log.info("[Agent] AI响应成功, hasToolCalls={}", message.containsKey("tool_calls"));
                    return message;
                }
            }

            String err = "AI服务返回空响应";
            log.error("[Agent调用失败] {} 原始响应: {}", err, result);
            errorResult.put("error", err);
            errorResult.put("detail", "choices为空或message为空，原始响应: " + result.substring(0, Math.min(result.length(), 500)));
            return errorResult;

        } catch (java.net.SocketTimeoutException e) {
            String err = "AI服务连接超时";
            log.error("[Agent调用失败] {} - {}", err, e.getMessage());
            errorResult.put("error", err);
            errorResult.put("detail", "请求AI服务超时（connect/read timeout），请稍后重试。技术详情: " + e.getMessage());
            return errorResult;
        } catch (java.net.ConnectException e) {
            String err = "AI服务连接失败";
            log.error("[Agent调用失败] {} - {}", err, e.getMessage());
            errorResult.put("error", err);
            errorResult.put("detail", "无法连接到AI服务，请检查网络。技术详情: " + e.getMessage());
            return errorResult;
        } catch (Exception e) {
            String err = "AI调用异常: " + e.getClass().getSimpleName();
            log.error("[Agent调用失败] {}", err, e);
            errorResult.put("error", err);
            errorResult.put("detail", e.getMessage() != null ? e.getMessage() : e.toString());
            return errorResult;
        }
    }

    /**
     * 检测AI回复是否为过渡性回复（如"请稍等"、"我将调用工具"等）
     * 这类回复不是最终答案，需要强制AI继续处理
     */
    private boolean isIntermediateResponse(String content) {
        if (StrUtil.isBlank(content)) return false;
        String trimmed = content.trim();
        // 过渡性关键词
        String[] intermediatePatterns = {
            "请稍等", "请等待", "稍等一下", "请稍候",
            "我将调用", "我将查询", "我将获取", "我将为您",
            "接下来我将", "接下来我会", "接下来让我",
            "正在调用", "正在查询", "正在获取", "正在处理",
            "即将调用", "即将查询", "即将获取",
            "需要调用", "需要查询", "需要获取",
            "让我调用", "让我查询", "让我获取", "让我为您",
            "我需要先", "我需要调用", "我需要查询",
            "马上为您", "马上查询", "马上调用",
            "现在为您查询", "现在为您调用"
        };
        for (String pattern : intermediatePatterns) {
            if (trimmed.contains(pattern)) {
                return true;
            }
        }
        // 短回复（<100字）且不含数字/数据，大概率是过渡性回复
        if (trimmed.length() < 100 && !trimmed.matches(".*\\d{2,}.*")) {
            // 进一步检查是否只是表态而非数据
            String[] noDataPatterns = {"帮你", "为您", "一下", "处理", "操作"};
            for (String p : noDataPatterns) {
                if (trimmed.contains(p)) return true;
            }
        }
        return false;
    }

    /**
     * 构建工具调用指令提示词
     * 告诉AI如何使用选定的工具，以及如何输出结构化的工具调用请求
     */
    private String buildToolCallInstruction(String toolPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是ERP系统的AI助手。当用户需要查询数据时，你必须立即调用工具，不要询问用户确认。\n\n");
        sb.append("## 可用工具\n\n");
        sb.append(toolPrompt);
        sb.append("\n## 核心规则（必须严格遵守）\n\n");
        sb.append("### 调用工具时\n");
        sb.append("1. 你必须直接调用工具，不要询问用户是否需要调用\n");
        sb.append("2. 你必须直接调用工具，不要解释你将要做什么\n");
        sb.append("3. 你必须直接调用工具，不要展示参数说明\n");
        sb.append("4. 如果信息不足，使用合理的默认值直接调用\n\n");
        sb.append("### 输出格式（严格遵守）\n");
        sb.append("当你需要调用工具时，你的回复必须且只能包含以下格式：\n\n");
        sb.append("TOOL_CALL\n");
        sb.append("{\"tool\": \"工具名称\", \"params\": {\"参数名\": \"参数值\"}}\n\n");
        sb.append("绝对不要在TOOL_CALL之前或之后添加任何其他文字、解释、emoji、表格或JSON示例。\n\n");
        sb.append("### 正确示例\n");
        sb.append("用户：查询FD-APN安普纳的库存\n");
        sb.append("你的回复：\n");
        sb.append("TOOL_CALL\n");
        sb.append("{\"tool\": \"amz_query_amazon_group\", \"params\": {\"groupname\": \"FD-APN 安普纳\"}}\n\n");
        sb.append("### 错误示例（绝对不要这样做）\n");
        sb.append("❌ 询问用户：\"您需要我查询吗？\"\n");
        sb.append("❌ 解释参数：\"参数说明：groupname = ...\"\n");
        sb.append("❌ 展示JSON示例而不加TOOL_CALL前缀\n");
        sb.append("❌ 使用emoji或表格格式\n\n");
        sb.append("### 链式调用规则（非常重要）\n");
        sb.append("- 每次最多输出1个TOOL_CALL，等收到结果后再决定是否需要下一个\n");
        sb.append("- 不要一次输出多个TOOL_CALL，系统会逐个处理\n");
        sb.append("- 收到工具结果后，优先考虑直接输出最终答案\n");
        sb.append("- 只有在数据明显不足时才继续调用工具\n");
        sb.append("- 绝对不要重复调用已经调用过的相同工具和参数\n");
        sb.append("- 收到工具结果后，自动执行下一步，不要停下来等用户确认\n");
        sb.append("- 如果工具调用失败，自动修正参数重试，不要让用户手动确认\n");
        sb.append("- 绝对不要输出\"接下来我将...\"、\"请稍等\"、\"确认后继续\"之类的文字\n\n");
        sb.append("### 店铺查询规则（非常重要）\n");
        sb.append("- 当用户提到店铺名（如\"kuuqa\"、\"我的店铺\"等）时，你必须先调用 amz_query_amazon_group 获取店铺的 groupid\n");
        sb.append("- 绝对不要把店铺名直接当作groupid使用！groupid是一串数字ID，不是店铺名\n");
        sb.append("- 正确流程：先调用 amz_query_amazon_group 获取groupid → 再用groupid调用其他工具\n\n");
        sb.append("### 店铺查询正确示例\n");
        sb.append("用户：我的店铺kuuqa这个月销售情况\n");
        sb.append("第1步回复：\n");
        sb.append("TOOL_CALL\n");
        sb.append("{\"tool\": \"amz_query_amazon_group\", \"params\": {\"groupname\": \"kuuqa\"}}\n\n");
        sb.append("收到groupid结果后，第2步回复（根据工具定义使用正确参数）：\n");
        sb.append("TOOL_CALL\n");
        sb.append("{\"tool\": \"amz_query_order_summary\", \"params\": {\"groupid\": \"从上一步获取的数字ID\", \"fromDate\": \"2026-07-01\", \"endDate\": \"2026-07-18\"}}\n\n");
        sb.append("### 其他规则\n");
        sb.append("- 如果不需要调用工具，直接回答用户的问题\n");
        sb.append("- shopid等租户参数由系统自动注入，不需要传\n");
        sb.append("- 查询正常数据时必须传 isDelete=0\n");
        sb.append("- 不要编造数据，必须通过工具获取真实数据\n");
        sb.append("- 当前日期：").append(java.time.LocalDate.now()).append("\n");
        return sb.toString();
    }

    /**
     * 从AI的文本响应中解析工具调用请求
     * 支持多种格式：
     * 1. TOOL_CALL\n{"tool": "...", "params": {...}}
     * 2. 直接的JSON对象 {"tool": "...", "params": {...}}
     * 3. 代码块中的JSON ```{...}```
     */
    private List<JSONObject> parseToolCallsFromText(String text) {
        List<JSONObject> toolCalls = new ArrayList<>();
        if (StrUtil.isBlank(text)) return toolCalls;

        try {
            // 方法1：查找 TOOL_CALL 标记后的JSON对象（标准格式）
            parseWithMarker(text, "TOOL_CALL", toolCalls);

            // 方法2：解析非标准格式 "TOOL_CALL toolName params: {...}"
            if (toolCalls.isEmpty()) {
                parseNonStandardFormat(text, toolCalls);
            }

            // 方法3：如果没有通过标记找到，尝试从文本中直接提取包含"tool"键的JSON对象
            if (toolCalls.isEmpty()) {
                parseJsonWithToolKey(text, toolCalls);
            }

            // 方法4：查找代码块中的JSON
            if (toolCalls.isEmpty()) {
                parseFromCodeBlocks(text, toolCalls);
            }

            log.info("[Agent] 从AI响应中解析到{}个工具调用", toolCalls.size());
        } catch (Exception e) {
            log.error("[Agent] 解析工具调用失败", e);
        }
        return toolCalls;
    }

    /**
     * 方法1：通过TOOL_CALL标记解析
     */
    private void parseWithMarker(String text, String marker, List<JSONObject> toolCalls) {
        int searchFrom = 0;
        while (true) {
            int idx = text.indexOf(marker, searchFrom);
            if (idx < 0) break;

            int jsonStart = text.indexOf("{", idx + marker.length());
            if (jsonStart < 0) break;

            JSONObject result = extractJsonObject(text, jsonStart);
            if (result != null) {
                JSONObject toolCall = result.getJSONObject("json");
                // 支持多种键名格式: "tool"/"name" 和 "params"/"arguments"
                if (toolCall.containsKey("tool") || toolCall.containsKey("name")) {
                    // 统一键名
                    if (!toolCall.containsKey("tool") && toolCall.containsKey("name")) {
                        toolCall.put("tool", toolCall.getString("name"));
                        toolCall.remove("name");
                    }
                    if (!toolCall.containsKey("params") && toolCall.containsKey("arguments")) {
                        toolCall.put("params", toolCall.get("arguments"));
                        toolCall.remove("arguments");
                    }
                    normalizeToolCall(toolCall);
                    toolCalls.add(toolCall);
                    log.info("[Agent] [标记解析] 解析到工具调用: {}", toolCall.getString("tool"));
                }
                searchFrom = result.getIntValue("end") + 1;
            } else {
                searchFrom = idx + marker.length();
            }
        }
    }

    /**
     * 方法2：解析非标准格式 "TOOL_CALL toolName params: {...}" 或 "TOOL_CALL toolName {params}"
     * AI有时会输出这种格式而不是标准的JSON格式
     */
    private void parseNonStandardFormat(String text, List<JSONObject> toolCalls) {
        // 匹配 TOOL_CALL 后跟工具名和参数的格式
        // 例如: TOOL_CALL amz_query_order_total params: {"groupid": "kuuqa"}
        // 或: TOOL_CALL amz_query_order_total {"groupid": "kuuqa"}
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "TOOL_CALL\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s+(?:params:?\\s*)?(\\{[^}]+\\})",
            java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String toolName = matcher.group(1);
            String paramsJson = matcher.group(2);
            try {
                JSONObject params = JSONObject.parseObject(paramsJson);
                JSONObject toolCall = new JSONObject();
                toolCall.put("tool", toolName);
                toolCall.put("params", params);
                toolCalls.add(toolCall);
                log.info("[Agent] [非标格式解析] 解析到工具调用: {}, params: {}", toolName, paramsJson);
            } catch (Exception e) {
                log.warn("[Agent] [非标格式解析] 解析参数JSON失败: {}", paramsJson);
            }
        }
    }

    /**
     * 方法3：直接从文本中查找包含"tool"或"name"键的JSON对象
     */
    private void parseJsonWithToolKey(String text, List<JSONObject> toolCalls) {
        // 查找所有可能的JSON对象起始位置
        int searchFrom = 0;
        Set<String> foundTools = new HashSet<>();

        while (searchFrom < text.length()) {
            int jsonStart = text.indexOf("{", searchFrom);
            if (jsonStart < 0) break;

            JSONObject result = extractJsonObject(text, jsonStart);
            if (result == null) {
                searchFrom = jsonStart + 1;
                continue;
            }

            JSONObject json = result.getJSONObject("json");
            // 检查是否包含"tool"或"name"键，且看起来是工具调用（有params或arguments）
            boolean hasToolName = json.containsKey("tool") || json.containsKey("name");
            boolean hasParams = json.containsKey("params") || json.containsKey("arguments");
            if (hasToolName && hasParams) {
                // 统一键名
                if (!json.containsKey("tool") && json.containsKey("name")) {
                    json.put("tool", json.getString("name"));
                    json.remove("name");
                }
                if (!json.containsKey("params") && json.containsKey("arguments")) {
                    json.put("params", json.get("arguments"));
                    json.remove("arguments");
                }
                String toolName = json.getString("tool");
                if (!foundTools.contains(toolName)) {
                    normalizeToolCall(json);
                    toolCalls.add(json);
                    foundTools.add(toolName);
                    log.info("[Agent] [JSON解析] 解析到工具调用: {}", toolName);
                }
            }

            searchFrom = result.getIntValue("end") + 1;
        }
    }

    /**
     * 方法3：从代码块中解析JSON
     */
    private void parseFromCodeBlocks(String text, List<JSONObject> toolCalls) {
        // 查找 ``` 包裹的代码块
        int idx = 0;
        while (true) {
            int codeStart = text.indexOf("```", idx);
            if (codeStart < 0) break;
            int codeEnd = text.indexOf("```", codeStart + 3);
            if (codeEnd < 0) break;

            String codeBlock = text.substring(codeStart + 3, codeEnd).trim();
            // 跳过语言标记（如json）
            if (codeBlock.startsWith("json\n") || codeBlock.startsWith("JSON\n")) {
                codeBlock = codeBlock.substring(5);
            }

            try {
                JSONObject json = JSONObject.parseObject(codeBlock.trim());
                if (json != null && json.containsKey("tool")) {
                    normalizeToolCall(json);
                    toolCalls.add(json);
                    log.info("[Agent] [代码块解析] 解析到工具调用: {}", json.getString("tool"));
                }
            } catch (Exception ignored) {
            }

            idx = codeEnd + 3;
        }
    }

    /**
     * 从文本中提取JSON对象
     */
    private JSONObject extractJsonObject(String text, int start) {
        int braceCount = 0;
        int end = -1;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    end = i;
                    break;
                }
            }
        }

        if (end > start) {
            String jsonStr = text.substring(start, end + 1);
            try {
                JSONObject json = JSONObject.parseObject(jsonStr);
                JSONObject result = new JSONObject();
                result.put("json", json);
                result.put("end", end);
                return result;
            } catch (Exception e) {
                log.warn("[Agent] 解析JSON失败: {}", jsonStr.length() > 100 ? jsonStr.substring(0, 100) + "..." : jsonStr);
            }
        }
        return null;
    }

    /**
     * 标准化工具调用JSON，确保params格式正确
     */
    private void normalizeToolCall(JSONObject toolCall) {
        // 兼容 arguments 和 params 两种字段名
        if (!toolCall.containsKey("params") && toolCall.containsKey("arguments")) {
            toolCall.put("params", toolCall.get("arguments"));
        }
        Object params = toolCall.get("params");
        if (params instanceof JSONObject) {
            toolCall.put("params", ((JSONObject) params).toJSONString());
        } else if (params instanceof String) {
            // 已经是字符串，保持不变
        } else if (params == null) {
            toolCall.put("params", "{}");
        }
    }

    /**
     * 从AI响应中提取思考内容（TOOL_CALL标记之前的部分）
     */
    private String extractThinkingContent(String text) {
        if (StrUtil.isBlank(text)) return "";
        int idx = text.indexOf("TOOL_CALL");
        if (idx > 0) {
            // TOOL_CALL前面有文字，提取为思考内容
            String thinking = text.substring(0, idx).trim();
            // 过滤掉过渡性文字
            if (isIntermediateResponse(thinking)) {
                return "";
            }
            return thinking;
        }
        // TOOL_CALL在开头或不存在，没有思考内容
        return "";
    }

    // ==================== P1: 学习分析 ====================

    /**
     * 纠正信号关键词模式
     */
    private static final String[] CORRECTION_PATTERNS = {
            "你应该用", "不要用", "应该调用", "用错了", "不对",
            "正确的做法", "改成用", "应该先", "再用",
            "不需要传", "参数是", "不是.*而是",
            "用这个", "换.*接口", "换成", "用错",
            "不是.*是", "应该用.*查", "应该用.*接口"
    };

    /**
     * 对话结束后异步分析：检测用户是否纠正了AI的工具选择或参数使用
     * 如果检测到纠正，提取学习内容并写入 t_sys_agent_learning
     */
    private void analyzeAndRecordLearning(String userMessage, String aiReply, String sessionId, UserInfo userInfo) {
        if (StrUtil.isBlank(userMessage)) return;

        // 第1步：关键词粗筛 - 检测是否包含纠正信号
        boolean hasCorrectionSignal = false;
        for (String pattern : CORRECTION_PATTERNS) {
            if (userMessage.matches(".*" + pattern + ".*")) {
                hasCorrectionSignal = true;
                break;
            }
        }
        if (!hasCorrectionSignal) return;

        log.info("[学习] 检测到纠正信号，用户消息: {}", userMessage.substring(0, Math.min(userMessage.length(), 100)));

        // 第2步：用轻量AI分析纠正内容，提取结构化学习记录
        try {
            JSONObject learningRecord = extractLearningWithAI(userMessage, aiReply);
            if (learningRecord == null) return;

            // 第3步：保存学习记录
            IToolAgentLearningService learningService = SpringUtil.getBean(IToolAgentLearningService.class);
            ToolAgentLearning learning = new ToolAgentLearning();
            learning.setLearnType(learningRecord.getString("learn_type"));
            learning.setLearnKey(learningRecord.getString("learn_key"));
            learning.setContent(learningRecord.getString("content"));
            learning.setSourceType(ToolAgentLearning.SOURCE_USER_CORRECTION);
            learning.setSourceSessionId(sessionId);
            learning.setEffectiveScope(ToolAgentLearning.SCOPE_GLOBAL);
            learning.setConfidence(new java.math.BigDecimal("0.60")); // 用户纠正的初始置信度比自动发现高
            learning.setStatus(ToolAgentLearning.STATUS_PENDING);
            learning.setTriggerPatterns(learningRecord.getString("trigger_patterns"));

            boolean saved = learningService.recordLearning(learning);
            if (saved) {
                log.info("[学习] 成功记录学习: type={}, key={}", learning.getLearnType(), learning.getLearnKey());
            }

        } catch (Exception e) {
            log.warn("[学习] 提取学习记录失败: {}", e.getMessage());
        }
    }

    /**
     * 用轻量AI从用户纠正消息中提取结构化学习记录
     */
    private JSONObject extractLearningWithAI(String userMessage, String aiReply) {
        String prompt = "分析以下对话，用户是否纠正了AI的工具选择或参数使用？\n" +
                "如果是，输出JSON格式的学习记录（只输出JSON，不要其他文字）：\n" +
                "{\n" +
                "  \"learn_type\": \"tool_routing 或 param_correction 或 multi_step_flow\",\n" +
                "  \"learn_key\": \"类型:简短描述（如 tool_routing:erp_material_query）\",\n" +
                "  \"content\": {JSON格式的学习内容},\n" +
                "  \"trigger_patterns\": [触发关键词列表]\n" +
                "}\n" +
                "如果不是纠正，输出 null\n\n" +
                "用户消息: " + userMessage + "\n" +
                "AI回复: " + (aiReply != null ? aiReply.substring(0, Math.min(aiReply.length(), 500)) : "");

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "qwen-turbo");
            requestBody.put("messages", Collections.singletonList(Map.of("role", "user", "content", prompt)));
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 500);

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestBody.toJSONString());

            Request request = new Request.Builder()
                    .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            OkHttpClient client = getClient();
            Response response = client.newCall(request).execute();
            if (response == null || !response.isSuccessful() || response.body() == null) {
                return null;
            }

            String result = response.body().string();
            JSONObject resultJson = JSONObject.parseObject(result);
            JSONArray choices = resultJson.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) return null;

            String content = choices.getJSONObject(0).getJSONObject("message").getString("content");
            if (content == null || content.trim().equals("null") || content.trim().isEmpty()) return null;

            // 提取JSON部分
            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            }
            // 找到第一个 { 和最后一个 }
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                content = content.substring(start, end + 1);
            }

            JSONObject parsed = JSONObject.parseObject(content);
            if (parsed != null && parsed.containsKey("learn_type") && parsed.containsKey("learn_key")) {
                return parsed;
            }
            return null;

        } catch (Exception e) {
            log.warn("[学习] AI提取学习记录失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 发送状态事件到前端
     */
    private void sendStatusEvent(SseEmitter emitter, String status, String message) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", "status");
            event.put("status", status);
            event.put("message", message);
            emitter.send(SseEmitter.event()
                    .id("status_" + System.currentTimeMillis())
                    .data(event.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
            log.debug("[Agent] 发送状态事件: {} - {}", status, message);
        } catch (IOException e) {
            log.warn("[Agent] 发送状态事件失败: {}", e.getMessage());
        }
    }

    /**
     * 发送Agent详细执行事件到前端（用于执行过程面板）
     */
    private void sendAgentDetailEvent(SseEmitter emitter, String type, String message) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", type);
            event.put("message", message);
            event.put("timestamp", System.currentTimeMillis());
            emitter.send(SseEmitter.event()
                    .id(type + "_" + System.currentTimeMillis())
                    .data(event.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("[Agent] 发送Agent详细事件失败: {}", e.getMessage());
        }
    }

    /**
     * 发送错误事件到前端
     */
    private void sendErrorEvent(SseEmitter emitter, String error, String detail) {
        try {
            JSONObject event = new JSONObject();
            event.put("type", "error");
            event.put("message", error);
            event.put("detail", detail);
            emitter.send(SseEmitter.event()
                    .id("error_" + System.currentTimeMillis())
                    .data(event.toJSONString(), org.springframework.http.MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.warn("[Agent] 发送错误事件失败: {}", e.getMessage());
        }
    }

}
