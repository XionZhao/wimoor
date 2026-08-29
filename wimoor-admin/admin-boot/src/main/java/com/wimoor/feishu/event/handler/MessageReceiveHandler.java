package com.wimoor.feishu.event.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.im.ImService;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1;
import com.lark.oapi.service.im.v1.model.P2MessageReceiveV1Data;
import com.wimoor.common.user.UserInfo;
import com.wimoor.admin.mapper.SysUserShopMapper;
import com.wimoor.admin.pojo.entity.SysUser;
import com.wimoor.admin.service.ISysUserService;
import com.wimoor.feishu.mapper.FeishuChatMemberMapper;
import com.wimoor.feishu.pojo.entity.Auth;
import com.wimoor.feishu.pojo.entity.FeishuChatMember;
import com.wimoor.feishu.service.IAuthService;
import com.wimoor.feishu.service.IFeishuChatService;
import com.wimoor.sys.tool.pojo.dto.SysChartCompletionRequestDTO;
import com.wimoor.sys.tool.pojo.entity.DeepSeekMessage;
import com.wimoor.sys.tool.pojo.entity.DeepseekChatMessages;
import com.wimoor.sys.tool.pojo.entity.DeepseekChatSessions;
import com.wimoor.sys.tool.service.ISysDeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书消息接收事件处理器
 * 处理 im.message.receive_v1 事件
 *
 * 逻辑：
 * 1. 群聊消息 -> 保存到数据库
 * 2. 私聊消息（p2p）-> 调用DeepSeek Agent Stream API回复
 */
@Slf4j
@Component
public class MessageReceiveHandler extends ImService.P2MessageReceiveV1Handler {

    @Autowired
    private IFeishuChatService feishuChatService;

    @Autowired
    private IAuthService authService;

    // 消息去重：记录已处理的messageId，防止飞书重试导致重复回复
    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> processedMessages = new java.util.concurrent.ConcurrentHashMap<>();
    // 记录机器人自己发出的消息ID，防止机器人回复触发新的消息事件
    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> botSentMessages = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    private ISysDeepSeekService deepSeekService;

    @Autowired
    private SysUserShopMapper sysUserShopMapper;

    @Autowired
    private FeishuChatMemberMapper feishuChatMemberMapper;

    @Autowired
    private ISysUserService sysUserService;

    private String currentAppId;

    public void setCurrentAppId(String appId) {
        this.currentAppId = appId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(P2MessageReceiveV1 event) throws Exception {
        try {
            log.info("收到飞书消息事件: {}", Jsons.DEFAULT.toJson(event.getEvent()));

            P2MessageReceiveV1Data data = event.getEvent();
            if (data == null) {
                log.warn("飞书消息事件data为空");
                return;
            }

            // 获取消息和发送者信息
            Map<String, Object> messageMap = JSONObject.parseObject(
                    Jsons.DEFAULT.toJson(data.getMessage()), Map.class);
            Map<String, Object> senderMap = JSONObject.parseObject(
                    Jsons.DEFAULT.toJson(data.getSender()), Map.class);

            if (messageMap == null) {
                log.warn("飞书消息事件message为空");
                return;
            }

            String messageId = (String) messageMap.get("message_id");
            String chatId = (String) messageMap.get("chat_id");
            String chatType = (String) messageMap.get("chat_type");
            String msgType = (String) messageMap.get("message_type");
            String content = (String) messageMap.get("content");

            // 消息去重：防止飞书重试导致重复回复
            if (processedMessages.containsKey(messageId)) {
                log.info("消息已处理过，跳过: messageId={}", messageId);
                return;
            }
            processedMessages.put(messageId, true);
            // 5分钟后清理，防止内存泄漏
            final String msgId = messageId;
            new Thread(() -> {
                try { Thread.sleep(300000); } catch (InterruptedException ignored) {}
                processedMessages.remove(msgId);
            }).start();

            // 检查是否是机器人自己发的消息（防止循环回复）
            if (botSentMessages.containsKey(messageId)) {
                log.info("这是机器人自己发的消息，跳过: messageId={}", messageId);
                return;
            }

            log.info("飞书消息: messageId={}, chatId={}, chatType={}, msgType={}",
                    messageId, chatId, chatType, msgType);

            // 忽略机器人自己发的消息，防止无限循环
            String senderType = (String) senderMap.get("sender_type");
            if (!"user".equals(senderType)) {
                log.info("忽略非用户消息: senderType={}", senderType);
                return;
            }

            // 提取发送者openId
            String senderOpenId = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> senderIdMap = (Map<String, Object>) senderMap.get("sender_id");
            if (senderIdMap != null) {
                senderOpenId = (String) senderIdMap.get("open_id");
            }

            // 构建eventJson给Service处理
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("message", messageMap);
            eventData.put("sender", senderMap);
            Map<String, Object> eventJson = new HashMap<>();
            eventJson.put("event", eventData);

            // 1. 保存到数据库
            feishuChatService.handleReceiveMessage(eventJson, currentAppId);

            // 2. 异步调用DeepSeek回复（避免阻塞事件回调，飞书3秒超时会重试）
            final String fChatId = chatId;
            final String fMsgType = msgType;
            final String fContent = content;
            final String fChatType = chatType;
            final String fMessageId = messageId;
            final Map<String, Object> fMessageMap = messageMap;
            final String fSenderOpenId = senderOpenId;
            new Thread(() -> {
                try {
                    if ("p2p".equals(fChatType)) {
                        handleP2pMessage(fChatId, fMsgType, fContent, fSenderOpenId);
                    } else if ("group".equals(fChatType)) {
                        handleGroupMessage(fMessageId, fChatId, fMsgType, fContent, fMessageMap, fSenderOpenId);
                    }
                } catch (Exception e) {
                    log.error("异步AI回复失败", e);
                }
            }).start();

        } catch (Exception e) {
            log.error("处理飞书消息接收事件失败", e);
        }
    }

    /**
     * 处理群聊消息：检查是否@了机器人，如果是则调用AI回复
     */
    @SuppressWarnings("unchecked")
    private void handleGroupMessage(String messageId, String chatId, String msgType, String content, Map<String, Object> messageMap, String senderOpenId) {
        try {
            if (!"text".equals(msgType)) {
                log.info("非文本消息，跳过AI回复: msgType={}", msgType);
                return;
            }

            // 检查是否有mentions（@）
            Object mentionsObj = messageMap.get("mentions");
            if (mentionsObj == null) {
                log.info("群聊消息没有@任何人，跳过AI回复");
                return;
            }

            // 获取机器人的open_id
            String botOpenId = getBotOpenId(currentAppId);
            if (StrUtil.isBlank(botOpenId)) {
                log.error("无法获取机器人open_id: appId={}", currentAppId);
                return;
            }

            // 检查mentions中是否包含机器人
            List<Map<String, Object>> mentions = (List<Map<String, Object>>) mentionsObj;
            boolean isMentioned = false;
            for (Map<String, Object> mention : mentions) {
                Map<String, Object> idMap = (Map<String, Object>) mention.get("id");
                if (idMap != null) {
                    String openId = (String) idMap.get("open_id");
                    if (botOpenId.equals(openId)) {
                        isMentioned = true;
                        log.info("检测到@机器人: botOpenId={}", botOpenId);
                        break;
                    }
                }
            }

            // 如果没有@机器人，跳过AI回复
            if (!isMentioned) {
                log.info("群聊消息没有@机器人，跳过AI回复: botOpenId={}", botOpenId);
                return;
            }

            // 提取文本内容，移除@标记
            String textContent = "";
            if (StrUtil.isNotBlank(content)) {
                JSONObject contentJson = JSONObject.parseObject(content);
                textContent = contentJson.getString("text");
                // 移除@标记 @_user_1 等
                if (textContent != null) {
                    textContent = textContent.replaceAll("@_user_\\d+", "").trim();
                }
            }

            if (StrUtil.isBlank(textContent)) {
                log.info("消息内容为空，跳过AI回复");
                return;
            }

            log.info("群聊消息@机器人，调用DeepSeek Agent Stream API回复: chatId={}, text={}", chatId, textContent);
            callDeepSeekAgentStream(currentAppId, chatId, textContent, senderOpenId);

        } catch (Exception e) {
            log.error("处理群聊消息失败", e);
        }
    }

    /**
     * 处理私聊消息：调用DeepSeek Agent Stream API回复
     */
    private void handleP2pMessage(String chatId, String msgType, String content, String senderOpenId) {
        try {
            if (!"text".equals(msgType)) {
                log.info("非文本消息，跳过AI回复: msgType={}", msgType);
                return;
            }

            String textContent = "";
            if (StrUtil.isNotBlank(content)) {
                JSONObject contentJson = JSONObject.parseObject(content);
                textContent = contentJson.getString("text");
            }

            if (StrUtil.isBlank(textContent)) {
                log.info("消息内容为空，跳过AI回复");
                return;
            }

            log.info("调用DeepSeek Agent Stream API回复私聊消息: chatId={}, text={}", chatId, textContent);
            callDeepSeekAgentStream(currentAppId, chatId, textContent, senderOpenId);

        } catch (Exception e) {
            log.error("处理私聊消息失败", e);
        }
    }

    /**
     * 调用DeepSeek Agent Stream API获取回复并通过飞书API发送
     */
    private void callDeepSeekAgentStream(String appId, String chatId, String text, String senderOpenId) {
        try {
            // 通过appId获取shopid和绑定信息（使用最早的记录）
            Auth auth = authService.lambdaQuery()
                    .eq(Auth::getAppId, appId)
                    .orderByAsc(Auth::getOpttime)
                    .last("LIMIT 1")
                    .one();
            if (auth == null || StrUtil.isBlank(auth.getShopid())) {
                log.error("无法获取飞书应用配置: appId={}", appId);
                return;
            }

            String shopId = auth.getShopid();

            // 通过飞书成员名称+shopId查询系统用户ID
            BigInteger userId = null;
            if (StrUtil.isNotBlank(senderOpenId)) {
                // 从t_sys_feishu_chat_member中获取飞书成员名称
                FeishuChatMember member = feishuChatMemberMapper.selectById(chatId + "_" + senderOpenId);
                if (member != null && StrUtil.isNotBlank(member.getName())) {
                    userId = sysUserShopMapper.findByNameAndShopId(member.getName(), shopId);
                    log.info("通过飞书成员名称匹配系统用户: name={}, shopId={}, userId={}", member.getName(), shopId, userId);
                }
            }
    
            if (userId == null) {
                log.warn("无法获取用户，使用默认用户: shopId={}", shopId);
                UserInfo userInfo = new UserInfo();
                userInfo.setId("1");
                userInfo.setCompanyid(shopId);
                String replyText = callNormalAI(userInfo, text);
                if (StrUtil.isNotBlank(replyText)) {
                    sendFeishuMessage(appId, chatId, replyText);
                } else {
                    log.warn("AI回复内容为空");
                }
                return ;
            }
            // 构造UserInfo对象（使用真实的主账号ID）
            UserInfo userInfo = new UserInfo();
            userInfo.setId(userId.toString());
            userInfo.setCompanyid(shopId);
            // 查询用户类型，用于权限过滤
            SysUser sysUser = sysUserService.getUserAllById(userId.toString());
            if (sysUser != null) {
                userInfo=sysUserService.convertToUserInfo(sysUser);
            }
            // 构建请求DTO
            SysChartCompletionRequestDTO dto = new SysChartCompletionRequestDTO();
            dto.setModel("deepseek-v3.2");
            dto.setSessionId(null); // 每次新会话
            // 飞书消息场景不注入帮助文档，避免干扰问题意图
            dto.setHelpDocUrl(null);
            dto.setHelpDocLibrary(null);
            dto.setCurrentHelpDoc(null);
            // 构建消息
            List<DeepSeekMessage> messages = new ArrayList<>();
            // 添加系统提示词，要求用中文回复
            DeepSeekMessage systemMessage = new DeepSeekMessage();
            systemMessage.setRole("system");
            systemMessage.setContent("你是一个智能助手，请用中文回复用户的问题。回复要简洁明了。");
            messages.add(systemMessage);
            DeepSeekMessage userMessage = new DeepSeekMessage();
            userMessage.setRole("user");
            userMessage.setContent(text);
            messages.add(userMessage);
            dto.setMessages(messages);

            log.info("调用Agent服务: shopId={}, userId={}, text={}", shopId, userId, text);

            // 调用同步Agent服务（支持工具调用）
            String replyText;
            try {
                replyText = deepSeekService.completionsAgentSync(userInfo, dto);
                log.info("AgentSync返回: {}", StrUtil.isNotBlank(replyText) ? replyText.substring(0, Math.min(100, replyText.length())) : "空");
            } catch (Exception e) {
                log.error("completionsAgentSync调用异常", e);
                replyText = "";
            }

            // 如果Agent回复为空，尝试使用普通AI直接回复
            if (StrUtil.isBlank(replyText)) {
                log.info("Agent回复为空，尝试使用普通AI直接回复");
                try {
                    replyText = callNormalAI(userInfo, text);
                    log.info("callNormalAI返回: {}", StrUtil.isNotBlank(replyText) ? replyText.substring(0, Math.min(100, replyText.length())) : "空");
                } catch (Exception e) {
                    log.error("callNormalAI调用异常", e);
                }
            }

            if (StrUtil.isNotBlank(replyText)) {
                sendFeishuMessage(appId, chatId, replyText);
            } else {
                log.warn("AI回复内容为空");
            }

        } catch (Exception e) {
            log.error("调用DeepSeek Agent失败", e);
        }
    }

    /**
     * 从Agent返回结果中提取回复内容
     */
    @SuppressWarnings("unchecked")
    private String extractReplyFromResult(Object result) {
        if (result == null) {
            return "";
        }

        try {
            // 处理DeepseekChatSessions对象
            if (result instanceof DeepseekChatSessions) {
                DeepseekChatSessions session = (DeepseekChatSessions) result;
                List<DeepseekChatMessages> messages = session.getMessages();
                if (messages != null && !messages.isEmpty()) {
                    // 从后往前找最后一条assistant消息
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        DeepseekChatMessages msg = messages.get(i);
                        if ("assistant".equals(msg.getRole())) {
                            return msg.getContent();
                        }
                    }
                }
            }

            // 处理JSONObject类型
            if (result instanceof JSONObject) {
                JSONObject json = (JSONObject) result;
                if (json.containsKey("messages")) {
                    List<JSONObject> messages = json.getJSONArray("messages").toJavaList(JSONObject.class);
                    // 从后往前找最后一条assistant消息
                    for (int i = messages.size() - 1; i >= 0; i--) {
                        JSONObject msg = messages.get(i);
                        if ("assistant".equals(msg.getString("role"))) {
                            return msg.getString("content");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("提取回复内容失败", e);
        }
        return "";
    }

    /**
     * 调用普通AI（非Agent模式）直接回复
     */
    private String callNormalAI(UserInfo userInfo, String text) {
        try {
            SysChartCompletionRequestDTO dto = new SysChartCompletionRequestDTO();
            dto.setModel("deepseek-v3.2");
            dto.setSessionId(null);

            List<DeepSeekMessage> messages = new ArrayList<>();
            // 添加系统提示词，要求用中文回复
            DeepSeekMessage systemMessage = new DeepSeekMessage();
            systemMessage.setRole("system");
            systemMessage.setContent("你是一个智能助手，请用中文回复用户的问题。回复要简洁明了。");
            messages.add(systemMessage);
            DeepSeekMessage userMessage = new DeepSeekMessage();
            userMessage.setRole("user");
            userMessage.setContent(text);
            messages.add(userMessage);
            dto.setMessages(messages);

            Object result = deepSeekService.completions(userInfo, dto);
            return extractReplyFromResult(result);
        } catch (Exception e) {
            log.error("调用普通AI失败", e);
            return "";
        }
    }

    /**
     * 解析SSE流式响应，提取AI回复内容
     */
    private String parseSSEResponse(String sseResponse) {
        StringBuilder replyBuilder = new StringBuilder();
        try {
            String[] lines = sseResponse.split("\n");
            for (String line : lines) {
                if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        continue;
                    }
                    try {
                        JSONObject json = JSONObject.parseObject(data);
                        // 检查是否是内容片段
                        if (json.containsKey("choices")) {
                            JSONArray choices = json.getJSONArray("choices");
                            if (choices != null && choices.size() > 0) {
                                JSONObject choice = choices.getJSONObject(0);
                                if (choice.containsKey("delta")) {
                                    JSONObject delta = choice.getJSONObject("delta");
                                    if (delta.containsKey("content")) {
                                        String content = delta.getString("content");
                                        if (content != null) {
                                            replyBuilder.append(content);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析SSE响应失败", e);
        }
        return replyBuilder.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractReplyText(JSONObject result) {
        if (!result.containsKey("data")) return "";
        JSONObject data = result.getJSONObject("data");
        if (data == null || !data.containsKey("messages")) return "";
        List<JSONObject> messages = data.getJSONArray("messages").toJavaList(JSONObject.class);
        if (messages == null || messages.isEmpty()) return "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            JSONObject msg = messages.get(i);
            if ("assistant".equals(msg.getString("role"))) {
                return msg.getString("content");
            }
        }
        return "";
    }

    private void sendFeishuMessage(String appId, String chatId, String text) {
        try {
            Auth auth = authService.lambdaQuery().eq(Auth::getAppId, appId).orderByAsc(Auth::getOpttime).last("LIMIT 1").one();
            if (auth == null) return;

            String token = getTenantAccessToken(auth.getAppId(), auth.getAppSecret());
            if (StrUtil.isBlank(token)) {
                log.error("获取tenant_access_token失败");
                return;
            }

            JSONObject contentObj = new JSONObject();
            contentObj.put("text", text);

            JSONObject msgBody = new JSONObject();
            msgBody.put("receive_id", chatId);
            msgBody.put("msg_type", "text");
            msgBody.put("content", contentObj.toJSONString());

            HttpResponse resp = HttpRequest.post("https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=chat_id")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .body(msgBody.toJSONString())
                    .timeout(10000)
                    .execute();

            if (resp.getStatus() == 200) {
                log.info("飞书消息发送成功: chatId={}", chatId);
                // 记录机器人发出的消息ID，防止收到自己的消息事件时循环
                try {
                    JSONObject respBody = JSONObject.parseObject(resp.body());
                    if (respBody != null && respBody.containsKey("data")) {
                        JSONObject data = respBody.getJSONObject("data");
                        if (data != null && data.containsKey("message_id")) {
                            String sentMsgId = data.getString("message_id");
                            botSentMessages.put(sentMsgId, true);
                            log.info("记录机器人发出的消息ID: {}", sentMsgId);
                            // 5分钟后清理
                            new Thread(() -> {
                                try { Thread.sleep(300000); } catch (InterruptedException ignored) {}
                                botSentMessages.remove(sentMsgId);
                            }).start();
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析发送响应失败", e);
                }
            } else {
                log.error("飞书消息发送失败: status={}, body={}", resp.getStatus(), resp.body());
            }

        } catch (Exception e) {
            log.error("发送飞书消息失败", e);
        }
    }

    /**
     * 获取机器人的open_id
     * 通过飞书API获取机器人信息
     */
    private String getBotOpenId(String appId) {
        try {
            Auth auth = authService.lambdaQuery().eq(Auth::getAppId, appId).orderByAsc(Auth::getOpttime).last("LIMIT 1").one();
            if (auth == null) {
                log.error("未找到飞书应用配置: appId={}", appId);
                return null;
            }

            String token = getTenantAccessToken(auth.getAppId(), auth.getAppSecret());
            if (StrUtil.isBlank(token)) {
                log.error("获取tenant_access_token失败");
                return null;
            }

            // 调用飞书API获取机器人信息
            HttpResponse response = HttpRequest.get("https://open.feishu.cn/open-apis/bot/v3/info")
                    .header("Authorization", "Bearer " + token)
                    .timeout(10000)
                    .execute();

            if (response.getStatus() == 200) {
                JSONObject result = JSONObject.parseObject(response.body());
                if (result != null && result.containsKey("bot")) {
                    JSONObject bot = result.getJSONObject("bot");
                    String openId = bot.getString("open_id");
                    log.info("获取机器人open_id成功: openId={}", openId);
                    return openId;
                }
            }
            log.error("获取机器人信息失败: status={}, body={}", response.getStatus(), response.body());
        } catch (Exception e) {
            log.error("获取机器人open_id异常", e);
        }
        return null;
    }

    private String getTenantAccessToken(String appId, String appSecret) {
        try {
            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("app_secret", appSecret);

            HttpResponse response = HttpRequest.post("https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal")
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(10000)
                    .execute();

            if (response.getStatus() == 200) {
                JSONObject result = JSONObject.parseObject(response.body());
                return result.getString("tenant_access_token");
            }
        } catch (Exception e) {
            log.error("获取tenant_access_token失败", e);
        }
        return null;
    }
}
