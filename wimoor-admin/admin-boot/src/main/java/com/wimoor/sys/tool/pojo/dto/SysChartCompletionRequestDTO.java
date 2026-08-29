package com.wimoor.sys.tool.pojo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wimoor.sys.tool.pojo.entity.DeepSeekMessage;
import com.wimoor.sys.tool.pojo.entity.ResponseFormat;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("ds接收参数对象")
public class SysChartCompletionRequestDTO implements Serializable {

    private List<DeepSeekMessage> messages;

    private String sessionId;

    private String model;

    private Double frequencyPenalty;

    private Integer maxTokens;

    private Double presencePenalty;

    private ResponseFormat responseFormat;

    private Object stop;

    private Boolean stream;

    private Object streamOptions;

    private Double temperature;

    private Double topP;

    private Object tools;

    private String toolChoice;

    private Boolean logprobs;

    private Object topLogprobs;

    /**
     * 是否启用Agent模式（自动调用系统接口获取数据）
     */
    private Boolean agentMode;

    /**
     * 当前页面路径（用于AI感知用户所在页面，提供针对性指导）
     */
    private String currentPage;

    /**
     * 当前页面标题（用于查询帮助文档）
     */
    private String pageTitle;

    /**
     * 帮助文档URL（从字典表查询）
     */
    private String helpDocUrl;

    /**
     * 整个帮助文档库（JSON数组，包含所有页面的帮助文档信息）
     */
    private String helpDocLibrary;

    /**
     * 当前页面的帮助文档内容（JSON对象，包含docKey、title、content、category）
     */
    private String currentHelpDoc;
}
