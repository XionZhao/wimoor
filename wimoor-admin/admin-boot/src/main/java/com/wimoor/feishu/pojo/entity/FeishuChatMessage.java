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
 * 飞书聊天消息表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_sys_feishu_chat_message")
@ApiModel(value = "FeishuChatMessage对象", description = "飞书聊天消息")
public class FeishuChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty(value = "消息ID（飞书message_id）")
    private String id;

    @ApiModelProperty(value = "群组ID")
    private String chatId;

    @ApiModelProperty(value = "发送者ID（open_id）")
    private String senderId;

    @ApiModelProperty(value = "发送者名称")
    private String senderName;

    @ApiModelProperty(value = "消息类型")
    private String msgType;

    @ApiModelProperty(value = "消息内容（JSON格式）")
    private String content;

    @ApiModelProperty(value = "纯文本内容")
    private String contentText;

    @ApiModelProperty(value = "父消息ID")
    private String parentId;

    @ApiModelProperty(value = "根消息ID")
    private String rootId;

    @ApiModelProperty(value = "会话类型: p2p/group")
    private String chatType;

    @ApiModelProperty(value = "被@的用户列表(JSON数组)")
    private String mentionUsers;

    @ApiModelProperty(value = "消息创建时间")
    private Date createTime;

    @ApiModelProperty(value = "入库时间")
    private Date insertTime;

    @ApiModelProperty(value = "飞书应用ID")
    private String appId;
}
