package com.wimoor.feishu.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.common.mvc.FileUpload;
import com.wimoor.common.user.UserInfo;
import com.wimoor.feishu.mapper.FeishuChatFileMapper;
import com.wimoor.feishu.mapper.FeishuChatGroupMapper;
import com.wimoor.feishu.mapper.FeishuChatMemberMapper;
import com.wimoor.feishu.mapper.FeishuChatMessageMapper;
import com.wimoor.feishu.pojo.entity.*;
import com.wimoor.feishu.service.IAuthService;
import com.wimoor.feishu.service.IFeishuChatService;
import com.wimoor.sys.tool.pojo.entity.LargeFile;
import com.wimoor.sys.tool.service.ILargeFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 飞书聊天记录服务实现
 */
@Slf4j
@Service
public class FeishuChatServiceImpl extends ServiceImpl<FeishuChatMessageMapper, FeishuChatMessage> implements IFeishuChatService {

    @Autowired
    private FeishuChatGroupMapper chatGroupMapper;
    @Autowired
    private FeishuChatMemberMapper chatMemberMapper;
    @Autowired
    private FeishuChatMessageMapper chatMessageMapper;
    @Autowired
    private FeishuChatFileMapper chatFileMapper;
    @Autowired
    private IAuthService authService;
    @Autowired
    private ILargeFileService largeFileService;
    @Autowired
    private FileUpload fileUpload;

    @Override
    public List<Map<String, Object>> getGroupList(UserInfo userInfo, String appId) {
        return chatGroupMapper.getGroupListWithStats(appId, userInfo.getUserName());
    }

    @Override
    public FeishuChatGroup getGroupById(String chatId) {
        return chatGroupMapper.selectById(chatId);
    }

    @Override
    public List<FeishuChatMember> getMemberList(String chatId) {
        return chatMemberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FeishuChatMember>()
                        .eq("chat_id", chatId)
                        .orderByDesc("message_count")
        );
    }

    @Override
    public IPage<FeishuChatMessage> getMessagePage(String chatId, int pageNum, int pageSize, String keyword) {
        Page<FeishuChatMessage> page = new Page<>(pageNum, pageSize);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FeishuChatMessage> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("chat_id", chatId);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like("content_text", keyword).or().like("sender_name", keyword));
        }
        wrapper.orderByDesc("create_time");
        return chatMessageMapper.selectPage(page, wrapper);
    }

    @Override
    public List<FeishuChatFile> getFilesByMessageId(String messageId) {
        return chatFileMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FeishuChatFile>()
                        .eq("message_id", messageId)
        );
    }

    @Override
    public IPage<FeishuChatFile> getFilePage(String chatId, int pageNum, int pageSize) {
        Page<FeishuChatFile> page = new Page<>(pageNum, pageSize);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FeishuChatFile> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("chat_id", chatId);
        wrapper.orderByDesc("create_time");
        return chatFileMapper.selectPage(page, wrapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleReceiveMessage(Map<String, Object> eventJson, String appId) {
        try {
            log.info("handleReceiveMessage收到事件: appId={}, eventJson={}", appId, eventJson);

            Object eventObj = eventJson.get("event");
            if (eventObj == null) {
                log.warn("飞书事件event字段为空, eventJson keys={}", eventJson.keySet());
                return;
            }
            Map<String, Object> event = eventObj instanceof Map ? (Map<String, Object>) eventObj
                    : com.alibaba.fastjson.JSONObject.parseObject(eventObj.toString(), Map.class);

            log.info("解析后的event: keys={}", event.keySet());

            Object messageObj = event.get("message");
            Object senderObj = event.get("sender");
            Map<String, Object> message = messageObj instanceof Map ? (Map<String, Object>) messageObj
                    : (messageObj != null ? com.alibaba.fastjson.JSONObject.parseObject(messageObj.toString(), Map.class) : null);
            Map<String, Object> sender = senderObj instanceof Map ? (Map<String, Object>) senderObj
                    : (senderObj != null ? com.alibaba.fastjson.JSONObject.parseObject(senderObj.toString(), Map.class) : null);

            log.info("解析message={}, sender={}", message != null, sender != null);

            if (message == null) {
                log.warn("飞书事件message字段为空");
                return;
            }

            String messageId = (String) message.get("message_id");
            String chatId = (String) message.get("chat_id");
            String chatType = (String) message.get("chat_type");
            String msgType = (String) message.get("message_type");
            String contentStr = (String) message.get("content");
            String createTimeStr = message.get("create_time") != null ? message.get("create_time").toString() : null;
            String parentId = (String) message.get("parent_id");
            String rootId = (String) message.get("root_id");

            // 获取发送者信息
            String senderId = null;
            String senderName = null;
            if (sender != null) {
                Object senderIdObj = sender.get("sender_id");
                if (senderIdObj instanceof Map) {
                    senderId = (String) ((Map<String, Object>) senderIdObj).get("open_id");
                }
                if (sender.get("sender_name") != null) {
                    senderName = (String) sender.get("sender_name");
                }
            }

            // 如果sender_name为空，通过API获取
            if (StrUtil.isBlank(senderName) && StrUtil.isNotBlank(senderId)) {
                senderName = fetchUserName(senderId, appId);
            }

            // 1. 确保群组存在
            ensureGroupExists(chatId, chatType, appId);

            // 2. 确保成员存在
            ensureMemberExists(senderId, chatId, senderName, appId);

            // 3. 保存消息
            FeishuChatMessage chatMessage = new FeishuChatMessage();
            chatMessage.setId(messageId);
            chatMessage.setChatId(chatId);
            chatMessage.setSenderId(senderId);
            chatMessage.setSenderName(senderName);
            chatMessage.setMsgType(msgType);
            chatMessage.setContent(contentStr);
            chatMessage.setContentText(extractTextContent(contentStr, msgType));
            chatMessage.setParentId(parentId);
            chatMessage.setRootId(rootId);
            chatMessage.setChatType(chatType);
            chatMessage.setAppId(appId);
            chatMessage.setInsertTime(new Date());

            if (createTimeStr != null) {
                try {
                    chatMessage.setCreateTime(new Date(Long.parseLong(createTimeStr)));
                } catch (NumberFormatException e) {
                    chatMessage.setCreateTime(new Date());
                }
            } else {
                chatMessage.setCreateTime(new Date());
            }

            // 解析mention
            Object mentionsObj = message.get("mentions");
            if (mentionsObj instanceof List) {
                List<Map<String, Object>> mentions = (List<Map<String, Object>>) mentionsObj;
                if (!mentions.isEmpty()) {
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < mentions.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append("\"").append(mentions.get(i).get("id")).append("\"");
                    }
                    sb.append("]");
                    chatMessage.setMentionUsers(sb.toString());
                }
            }

            // 检查消息是否已存在（防止重复插入）
            FeishuChatMessage existingMessage = chatMessageMapper.selectById(messageId);
            if (existingMessage != null) {
                log.info("消息已存在，跳过插入: messageId={}", messageId);
                return;
            }

            int rows = chatMessageMapper.insert(chatMessage);
            log.info("消息插入结果: rows={}, messageId={}, chatId={}", rows, messageId, chatId);

            // 4. 处理文件/图片消息
            if ("image".equals(msgType) || "file".equals(msgType) || "audio".equals(msgType) || "media".equals(msgType)) {
                saveFileRecord(messageId, chatId, senderId, contentStr, msgType, appId);
            }

            // 5. 更新成员统计
            updateMemberStats(senderId, chatId);

            log.info("飞书消息保存成功: messageId={}, chatId={}, msgType={}", messageId, chatId, msgType);

        } catch (Exception e) {
            log.error("处理飞书消息事件失败", e);
        }
    }

    @Override
    public void syncGroupInfo(String chatId, String appId) {
        log.info("同步群组信息: chatId={}, appId={}", chatId, appId);
    }

    @Override
    public void downloadFile(String fileId, HttpServletResponse response) throws Exception {
        FeishuChatFile file = chatFileMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

        log.info("下载文件: fileId={}, downloadStatus={}, localPath={}", fileId, file.getDownloadStatus(), file.getLocalPath());

        // 如果已经下载过且有本地路径，直接重定向
        if (file.getDownloadStatus() == 1 && StrUtil.isNotBlank(file.getLocalPath())) {
            log.info("文件已下载，重定向到: {}", file.getLocalPath());
            response.sendRedirect(file.getLocalPath());
            return;
        }

        // 如果还没有下载到MinIO，触发下载
        downloadAndSaveToMinio(file);

        // 重新获取文件记录
        file = chatFileMapper.selectById(fileId);
        if (file.getDownloadStatus() == 1 && StrUtil.isNotBlank(file.getLocalPath())) {
            log.info("文件下载完成，重定向到: {}", file.getLocalPath());
            response.sendRedirect(file.getLocalPath());
        } else {
            throw new RuntimeException("文件下载失败，请稍后重试");
        }
    }

    private void ensureGroupExists(String chatId, String chatType, String appId) {
        FeishuChatGroup group = chatGroupMapper.selectById(chatId);
        if (group == null) {
            group = new FeishuChatGroup();
            group.setId(chatId);
            group.setChatMode(chatType);
            group.setAppId(appId);
            group.setStatus(1);
            group.setMemberCount(0);
            group.setCreateTime(new Date());
            group.setUpdateTime(new Date());
            // 尝试从飞书API获取群名称
            fetchGroupName(group, appId);
            chatGroupMapper.insert(group);
        } else if (StrUtil.isBlank(group.getName())) {
            // 已存在但没有名称，补充获取
            fetchGroupName(group, appId);
            chatGroupMapper.updateById(group);
        }
    }

    /**
     * 通过飞书API获取群组信息
     */
    private void fetchGroupName(FeishuChatGroup group, String appId) {
        try {
            List<Auth> auths = authService.lambdaQuery().eq(Auth::getAppId, appId).list();
            if (auths == null||auths.isEmpty()) return;
            Auth auth = auths.get(0);
            com.lark.oapi.Client client = com.lark.oapi.Client.newBuilder(auth.getAppId(), auth.getAppSecret()).build();
            com.lark.oapi.service.im.v1.model.GetChatReq req = com.lark.oapi.service.im.v1.model.GetChatReq.newBuilder()
                    .chatId(group.getId())
                    .build();
            com.lark.oapi.service.im.v1.model.GetChatResp resp = client.im().chat().get(req);

            if (resp.success() && resp.getData() != null) {
                com.lark.oapi.service.im.v1.model.GetChatRespBody data = resp.getData();
                group.setName(data.getName());
                group.setDescription(data.getDescription());
                group.setAvatarUrl(data.getAvatar());
                group.setOwnerId(data.getOwnerId());
                if (data.getChatMode() != null) {
                    group.setChatMode(data.getChatMode());
                }
                log.info("获取群组信息成功: chatId={}, name={}", group.getId(), group.getName());
            } else {
                log.warn("获取群组信息失败: chatId={}, code={}, msg={}", group.getId(),
                        resp.getCode(), resp.getMsg());
            }
        } catch (Exception e) {
            log.error("获取群组信息异常: chatId={}", group.getId(), e);
        }
    }

    /**
     * 通过飞书API获取用户信息
     */
    private String fetchUserName(String openId, String appId) {
        try {
            List<Auth> auths = authService.lambdaQuery().eq(Auth::getAppId, appId).list();
            if (auths == null || auths.isEmpty()) return null;
            Auth auth = auths.get(0);
            com.lark.oapi.Client client = com.lark.oapi.Client.newBuilder(auth.getAppId(), auth.getAppSecret()).build();
            com.lark.oapi.service.contact.v3.model.GetUserReq req = com.lark.oapi.service.contact.v3.model.GetUserReq.newBuilder()
                    .userId(openId)
                    .userIdType("open_id")
                    .build();
            com.lark.oapi.service.contact.v3.model.GetUserResp resp = client.contact().user().get(req);

            if (resp.success() && resp.getData() != null) {
                String name = resp.getData().getUser().getName();
                log.info("获取用户信息成功: openId={}, name={}", openId, name);
                return name;
            } else {
                log.warn("获取用户信息失败: openId={}, code={}, msg={}", openId, resp.getCode(), resp.getMsg());
            }
        } catch (Exception e) {
            log.error("获取用户信息异常: openId={}", openId, e);
        }
        return null;
    }

    private void ensureMemberExists(String senderId, String chatId, String senderName, String appId) {
        if (StrUtil.isBlank(senderId)) return;

        String memberId = chatId + "_" + senderId;
        FeishuChatMember member = chatMemberMapper.selectById(memberId);
        if (member == null) {
            // 如果没有名称，通过API获取
            if (StrUtil.isBlank(senderName)) {
                senderName = fetchUserName(senderId, appId);
            }
            member = new FeishuChatMember();
            member.setId(memberId);
            member.setChatId(chatId);
            member.setMemberId(senderId);
            member.setName(senderName);
            member.setMemberIdType("open_id");
            member.setIsBot(0);
            member.setMessageCount(0);
            member.setCreateTime(new Date());
            member.setUpdateTime(new Date());
            chatMemberMapper.insert(member);
        } else if (StrUtil.isBlank(member.getName()) && StrUtil.isNotBlank(senderName)) {
            // 更新成员名称
            member.setName(senderName);
            chatMemberMapper.updateById(member);
        }
    }

    private void updateMemberStats(String senderId, String chatId) {
        if (StrUtil.isBlank(senderId)) return;

        String memberId = chatId + "_" + senderId;
        chatMemberMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FeishuChatMember>()
                        .eq(FeishuChatMember::getId, memberId)
                        .setSql("message_count = message_count + 1")
                        .set(FeishuChatMember::getLastMessageTime, new Date())
        );
        FeishuChatMember member = chatMemberMapper.selectById(memberId);
        if (member != null && member.getFirstMessageTime() == null) {
            chatMemberMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FeishuChatMember>()
                            .eq(FeishuChatMember::getId, memberId)
                            .set(FeishuChatMember::getFirstMessageTime, new Date())
            );
        }
    }

    @SuppressWarnings("unchecked")
    private void saveFileRecord(String messageId, String chatId, String senderId,
                                String contentStr, String msgType, String appId) {
        try {
            Map<String, Object> content = com.alibaba.fastjson.JSONObject.parseObject(contentStr, Map.class);
            FeishuChatFile file = new FeishuChatFile();
            file.setMessageId(messageId);
            file.setChatId(chatId);
            file.setSenderId(senderId);
            file.setFileType(msgType);
            file.setAppId(appId);
            file.setCreateTime(new Date());
            file.setDownloadStatus(0);

            if ("image".equals(msgType)) {
                String imageKey = (String) content.get("image_key");
                file.setId(imageKey != null ? imageKey : messageId);
                file.setImageKey(imageKey);
            } else if ("file".equals(msgType)) {
                String fileKey = (String) content.get("file_key");
                String fileName = (String) content.get("file_name");
                file.setId(fileKey != null ? fileKey : messageId);
                file.setFileKey(fileKey);
                file.setFileName(fileName);
            } else if ("audio".equals(msgType)) {
                String fileKey = (String) content.get("file_key");
                file.setId(fileKey != null ? fileKey : messageId);
                file.setFileKey(fileKey);
            } else if ("media".equals(msgType)) {
                String fileKey = (String) content.get("file_key");
                file.setId(fileKey != null ? fileKey : messageId);
                file.setFileKey(fileKey);
            }

            chatFileMapper.insert(file);

            // 异步下载文件到MinIO
            downloadAndSaveToMinio(file);
        } catch (Exception e) {
            log.error("保存文件记录失败: messageId={}", messageId, e);
        }
    }

    /**
     * 获取tenant_access_token
     */
    private String getTenantAccessToken(String appId, String appSecret) {
        try {
            com.alibaba.fastjson.JSONObject body = new com.alibaba.fastjson.JSONObject();
            body.put("app_id", appId);
            body.put("app_secret", appSecret);

            cn.hutool.http.HttpResponse response = cn.hutool.http.HttpRequest.post("https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal")
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(10000)
                    .execute();

            if (response.getStatus() == 200) {
                com.alibaba.fastjson.JSONObject result = com.alibaba.fastjson.JSONObject.parseObject(response.body());
                return result.getString("tenant_access_token");
            }
        } catch (Exception e) {
            log.error("获取tenant_access_token失败", e);
        }
        return null;
    }

    /**
     * 异步下载飞书文件并保存到MinIO
     */
    private void downloadAndSaveToMinio(FeishuChatFile file) {
        try {
            String fileKey = file.getFileKey();
            String imageKey = file.getImageKey();
            String fileName = file.getFileName();
            String fileType = file.getFileType();
            String appId = file.getAppId();

            if (StrUtil.isBlank(fileKey) && StrUtil.isBlank(imageKey)) {
                log.warn("文件key为空，跳过下载: fileId={}", file.getId());
                return;
            }

            List<Auth> auths = authService.lambdaQuery().eq(Auth::getAppId, appId).list();
            if (auths == null || auths.isEmpty()) {
                log.warn("飞书应用配置不存在: appId={}", appId);
                return;
            }
            Auth auth = auths.get(0);

            // 获取tenant_access_token
            String token = getTenantAccessToken(auth.getAppId(), auth.getAppSecret());
            if (StrUtil.isBlank(token)) {
                log.error("获取tenant_access_token失败: appId={}", appId);
                return;
            }

            byte[] fileBytes = null;
            String downloadFileKey = null;

            if ("image".equals(fileType) && StrUtil.isNotBlank(imageKey)) {
                downloadFileKey = imageKey;
            } else if (StrUtil.isNotBlank(fileKey)) {
                downloadFileKey = fileKey;
            }

            if (downloadFileKey == null) {
                log.warn("没有有效的文件key: fileId={}", file.getId());
                return;
            }

            // 使用HTTP API下载文件资源
            String url = "https://open.feishu.cn/open-apis/im/v1/messages/" + file.getMessageId()
                    + "/resources/" + downloadFileKey + "?type=" + ("image".equals(fileType) ? "image" : "file");

            log.info("调用飞书下载API: url={}", url);
            cn.hutool.http.HttpResponse resp = cn.hutool.http.HttpRequest.get(url)
                    .header("Authorization", "Bearer " + token)
                    .timeout(30000)
                    .execute();

            if (resp.getStatus() == 200) {
                fileBytes = resp.bodyBytes();
                log.info("文件下载成功: fileId={}, size={}", file.getId(), fileBytes.length);
                if (fileName == null) fileName = downloadFileKey + ("image".equals(fileType) ? ".png" : "");
            } else {
                log.error("下载文件失败: fileId={}, status={}, body={}", file.getId(), resp.getStatus(), resp.body());
                return;
            }

            if (fileBytes == null || fileBytes.length == 0) {
                log.warn("下载的文件内容为空: fileId={}", file.getId());
                return;
            }

            // 上传到MinIO
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            String storageName = fileName;
            if (fileName.contains(".")) {
                String[] parts = fileName.split("\\.");
                String nameWithoutExt = parts[0];
                String ext = parts[parts.length - 1];
                storageName = nameWithoutExt + "_" + format.format(new Date()) + "." + ext;
            } else {
                storageName = fileName + "_" + format.format(new Date());
            }

            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            LargeFile largeFile = largeFileService.uploadLargeFile(inputStream, "feishu", auth.getShopid(), storageName);

            if (largeFile != null) {
                // 更新文件记录，保存URL和MinIO文件ID
                String fileUrl = fileUpload.getPictureImage(largeFile.getLocation());
                file.setLocalPath(fileUrl);
                file.setLocalFileId(largeFile.getId());
                file.setDownloadStatus(1);
                file.setFileSize((long) fileBytes.length);
                chatFileMapper.updateById(file);
                log.info("文件自动下载到MinIO成功: fileId={}, url={}", file.getId(), fileUrl);
            } else {
                log.error("文件上传MinIO失败: fileId={}", file.getId());
            }
        } catch (Exception e) {
            log.error("自动下载文件失败: fileId={}", file.getId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextContent(String contentStr, String msgType) {
        if (StrUtil.isBlank(contentStr)) return "";
        try {
            if ("text".equals(msgType)) {
                Map<String, Object> content = com.alibaba.fastjson.JSONObject.parseObject(contentStr, Map.class);
                return content.get("text") != null ? content.get("text").toString() : "";
            } else if ("post".equals(msgType)) {
                Map<String, Object> content = com.alibaba.fastjson.JSONObject.parseObject(contentStr, Map.class);
                StringBuilder sb = new StringBuilder();
                extractPostText(content, sb);
                return sb.toString();
            } else if ("interactive".equals(msgType)) {
                return "[卡片消息]";
            } else {
                return "[" + msgType + "消息]";
            }
        } catch (Exception e) {
            return contentStr;
        }
    }

    @SuppressWarnings("unchecked")
    private void extractPostText(Map<String, Object> content, StringBuilder sb) {
        if (content.containsKey("title")) {
            sb.append(content.get("title")).append("\n");
        }
        for (String lang : new String[]{"zh_cn", "en_us", "ja_jp"}) {
            Object langObj = content.get(lang);
            if (!(langObj instanceof Map)) continue;
            Map<String, Object> langContent = (Map<String, Object>) langObj;
            if (langContent.containsKey("title")) {
                sb.append(langContent.get("title")).append("\n");
            }
            Object contentObj = langContent.get("content");
            if (contentObj instanceof List) {
                List<List<Map<String, Object>>> contentList = (List<List<Map<String, Object>>>) contentObj;
                for (List<Map<String, Object>> line : contentList) {
                    for (Map<String, Object> item : line) {
                        if ("text".equals(item.get("tag"))) {
                            sb.append(item.get("text"));
                        } else if ("a".equals(item.get("tag"))) {
                            sb.append(item.get("text"));
                        } else if ("at".equals(item.get("tag"))) {
                            sb.append("@").append(item.get("user_name"));
                        }
                    }
                    sb.append("\n");
                }
            }
            break;
        }
    }
}
