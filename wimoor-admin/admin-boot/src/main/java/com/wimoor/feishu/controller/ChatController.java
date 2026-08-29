package com.wimoor.feishu.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.feishu.pojo.entity.FeishuChatFile;
import com.wimoor.feishu.pojo.entity.FeishuChatGroup;
import com.wimoor.feishu.pojo.entity.FeishuChatMember;
import com.wimoor.feishu.pojo.entity.FeishuChatMessage;
import com.wimoor.feishu.pojo.entity.Auth;
import com.wimoor.feishu.service.IAuthService;
import com.wimoor.feishu.service.IFeishuChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 飞书聊天记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/feishu/chat")
@Api(tags = "飞书聊天记录管理")
public class ChatController {

    @Autowired
    private IFeishuChatService feishuChatService;
    @Autowired
    private IAuthService authService;

    /**
     * 根据当前用户获取其飞书应用ID
     */
    private String getUserAppId() {
        UserInfo userInfo = UserInfoContext.get();
        Auth auth = authService.lambdaQuery().eq(Auth::getShopid, userInfo.getCompanyid()).one();
        return auth != null ? auth.getAppId() : null;
    }

    @GetMapping("/groups")
    @ApiOperation("获取群组列表")
    public Result<List<Map<String, Object>>> getGroupList() {
        String appId = getUserAppId();
        UserInfo userInfo = UserInfoContext.get();
        List<Map<String, Object>> list = feishuChatService.getGroupList(userInfo,appId);
        return Result.success(list);
    }

    @GetMapping("/group/{chatId}")
    @ApiOperation("获取群组详情")
    public Result<FeishuChatGroup> getGroup(@PathVariable String chatId) {
        FeishuChatGroup group = feishuChatService.getGroupById(chatId);
        return Result.success(group);
    }

    @GetMapping("/members")
    @ApiOperation("获取群成员列表")
    public Result<List<FeishuChatMember>> getMemberList(
            @ApiParam("群组ID") @RequestParam String chatId) {
        List<FeishuChatMember> list = feishuChatService.getMemberList(chatId);
        return Result.success(list);
    }

    @GetMapping("/messages")
    @ApiOperation("分页查询消息列表")
    public Result<IPage<FeishuChatMessage>> getMessagePage(
            @ApiParam("群组ID") @RequestParam String chatId,
            @ApiParam("页码") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam("每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @ApiParam("搜索关键词") @RequestParam(required = false) String keyword) {
        IPage<FeishuChatMessage> page = feishuChatService.getMessagePage(chatId, pageNum, pageSize, keyword);
        return Result.success(page);
    }

    @GetMapping("/files")
    @ApiOperation("分页查询文件列表")
    public Result<IPage<FeishuChatFile>> getFilePage(
            @ApiParam("群组ID") @RequestParam String chatId,
            @ApiParam("页码") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam("每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        IPage<FeishuChatFile> page = feishuChatService.getFilePage(chatId, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/files/{messageId}")
    @ApiOperation("获取消息关联的文件")
    public Result<List<FeishuChatFile>> getFilesByMessageId(@PathVariable String messageId) {
        List<FeishuChatFile> files = feishuChatService.getFilesByMessageId(messageId);
        return Result.success(files);
    }

    @GetMapping("/file/download/{fileId}")
    @ApiOperation("下载文件")
    public void downloadFile(@PathVariable String fileId, HttpServletResponse response) {
        try {
            feishuChatService.downloadFile(fileId, response);
        } catch (Exception e) {
            log.error("下载文件失败: fileId={}", fileId, e);
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":500,\"msg\":\"" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }
}
