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
 * 飞书群组表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_sys_feishu_chat_group")
@ApiModel(value = "FeishuChatGroup对象", description = "飞书群组")
public class FeishuChatGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty(value = "群组ID（飞书chat_id）")
    private String id;

    @ApiModelProperty(value = "群名称")
    private String name;

    @ApiModelProperty(value = "群描述")
    private String description;

    @ApiModelProperty(value = "群头像URL")
    private String avatarUrl;

    @ApiModelProperty(value = "群主open_id")
    private String ownerId;

    @ApiModelProperty(value = "群模式: group/p2p")
    private String chatMode;

    @ApiModelProperty(value = "成员数量")
    private Integer memberCount;

    @ApiModelProperty(value = "飞书应用ID")
    private String appId;

    @ApiModelProperty(value = "状态: 1正常 0已解散")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;
}
