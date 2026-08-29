package com.wimoor.feishu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.common.user.UserInfo;
import com.wimoor.feishu.pojo.entity.FeishuChatFile;
import com.wimoor.feishu.pojo.entity.FeishuChatGroup;
import com.wimoor.feishu.pojo.entity.FeishuChatMember;
import com.wimoor.feishu.pojo.entity.FeishuChatMessage;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 飞书聊天记录服务
 */
public interface IFeishuChatService extends IService<FeishuChatMessage> {

    /**
     * 获取群组列表（含统计信息）
     */
    List<Map<String, Object>> getGroupList(UserInfo user, String appId);

    /**
     * 获取群组详情
     */
    FeishuChatGroup getGroupById(String chatId);

    /**
     * 获取群成员列表
     */
    List<FeishuChatMember> getMemberList(String chatId);

    /**
     * 分页查询消息列表
     */
    IPage<FeishuChatMessage> getMessagePage(String chatId, int pageNum, int pageSize, String keyword);

    /**
     * 获取消息关联的文件列表
     */
    List<FeishuChatFile> getFilesByMessageId(String messageId);

    /**
     * 获取群组的文件列表
     */
    IPage<FeishuChatFile> getFilePage(String chatId, int pageNum, int pageSize);

    /**
     * 保存接收到的飞书消息事件
     * @param eventJson 飞书事件JSON
     * @param appId 飞书应用ID
     */
    void handleReceiveMessage(Map<String, Object> eventJson, String appId);

    /**
     * 同步群组信息
     */
    void syncGroupInfo(String chatId, String appId);

    /**
     * 下载文件
     */
    void downloadFile(String fileId, HttpServletResponse response) throws Exception;
}
