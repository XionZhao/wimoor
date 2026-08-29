package com.wimoor.sys.tool.controller;

import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.sys.tool.pojo.dto.SysChartCompletionRequestDTO;
import com.wimoor.sys.tool.service.ISysDeepSeekService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Api(tags = "AI Agent智能助手")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deepseek")
public class SysDeepSeekController {
    final ISysDeepSeekService iSysDeepSeekService;

    @ApiOperation("同步对话")
    @PostMapping("/search")
    public Result<?> searchAction(@RequestBody SysChartCompletionRequestDTO dto) {
        UserInfo userInfo = UserInfoContext.get();
        return Result.success(iSysDeepSeekService.completions(userInfo, dto));
    }

    @ApiOperation("流式对话")
    @PostMapping(value = "/search/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter searchStreamAction(@RequestBody SysChartCompletionRequestDTO dto) {
        UserInfo userInfo = UserInfoContext.get();
        return iSysDeepSeekService.completionsStream(userInfo, dto);
    }

    @ApiOperation("Agent模式流式对话（支持工具调用）")
    @PostMapping(value = "/search/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter searchAgentStreamAction(@RequestBody SysChartCompletionRequestDTO dto) {
        UserInfo userInfo = UserInfoContext.get();
        return iSysDeepSeekService.completionsAgentStream(userInfo, dto);
    }

    @ApiOperation("获取会话列表")
    @GetMapping("/getSession")
    public Result<?> getSessionAction() {
        UserInfo userInfo = UserInfoContext.get();
        return Result.success(iSysDeepSeekService.getSession(userInfo));
    }

    @ApiOperation("删除会话")
    @DeleteMapping("/deleteSession/{sessionId}")
    public Result<?> deleteSessionAction(@PathVariable String sessionId) {
        UserInfo userInfo = UserInfoContext.get();
        iSysDeepSeekService.deleteSession(userInfo, sessionId);
        return Result.success();
    }

    @ApiOperation("获取快捷关键词")
    @GetMapping("/getKey")
    public Result<?> getKeyAction() {
        UserInfo userInfo = UserInfoContext.get();
        return Result.success(iSysDeepSeekService.getKey(userInfo));
    }

    @ApiOperation("获取Agent可用工具列表")
    @GetMapping("/getTools")
    public Result<?> getToolsAction() {
        return Result.success(iSysDeepSeekService.getAgentTools());
    }
}
