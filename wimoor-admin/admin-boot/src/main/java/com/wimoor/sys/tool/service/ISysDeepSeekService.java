package com.wimoor.sys.tool.service;

import com.wimoor.common.user.UserInfo;
import com.wimoor.sys.tool.pojo.dto.SysChartCompletionRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ISysDeepSeekService {

    Object completions(UserInfo userInfo, SysChartCompletionRequestDTO dto);

    /**
     * 流式调用AI接口
     */
    SseEmitter completionsStream(UserInfo userInfo, SysChartCompletionRequestDTO dto);

    /**
     * Agent模式流式调用（支持工具调用）
     */
    SseEmitter completionsAgentStream(UserInfo userInfo, SysChartCompletionRequestDTO dto);

    /**
     * Agent模式同步调用（支持工具调用），返回最终回复文本
     */
    String completionsAgentSync(UserInfo userInfo, SysChartCompletionRequestDTO dto);

    Object getSession(UserInfo userInfo);

    void deleteSession(UserInfo userInfo, String sessionId);

    Object getKey(UserInfo userInfo);

    /**
     * 获取所有可用的Agent工具定义
     */
    List<Object> getAgentTools();
}
