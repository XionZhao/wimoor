package com.wimoor.feishu.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 飞书群成员表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_sys_feishu_chat_member")
@ApiModel(value = "FeishuChatMember对象", description = "飞书群成员")
public class FeishuChatMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty(value = "主键ID")
    private String id;

    @ApiModelProperty(value = "群组ID")
    private String chatId;

    @ApiModelProperty(value = "成员名称")
    private String name;

    @ApiModelProperty(value = "英文名")
    private String enName;

    @ApiModelProperty(value = "头像URL")
    private String avatarUrl;

    @ApiModelProperty(value = "ID类型: open_id/user_id/union_id")
    private String memberIdType;

    @ApiModelProperty(value = "成员ID")
    private String memberId;

    @ApiModelProperty(value = "租户key")
    private String tenantKey;

    @ApiModelProperty(value = "是否机器人: 0否 1是")
    private Integer isBot;

    @ApiModelProperty(value = "首次发言时间")
    private Date firstMessageTime;

    @ApiModelProperty(value = "最后发言时间")
    private Date lastMessageTime;

    @ApiModelProperty(value = "消息数量")
    private Integer messageCount;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
