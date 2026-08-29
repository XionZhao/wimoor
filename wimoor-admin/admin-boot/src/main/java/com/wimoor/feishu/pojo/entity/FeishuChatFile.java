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
 * 飞书聊天文件表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_sys_feishu_chat_file")
@ApiModel(value = "FeishuChatFile对象", description = "飞书聊天文件")
public class FeishuChatFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @ApiModelProperty(value = "主键ID")
    private String id;

    @ApiModelProperty(value = "关联消息ID")
    private String messageId;

    @ApiModelProperty(value = "群组ID")
    private String chatId;

    @ApiModelProperty(value = "发送者ID")
    private String senderId;

    @ApiModelProperty(value = "文件类型: image/file/audio/media")
    private String fileType;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty(value = "飞书文件key")
    private String fileKey;

    @ApiModelProperty(value = "飞书图片key")
    private String imageKey;

    @ApiModelProperty(value = "本地存储路径")
    private String localPath;

    @ApiModelProperty(value = "本地存储文件ID")
    private String localFileId;

    @ApiModelProperty(value = "音视频时长（秒）")
    private Integer duration;

    @ApiModelProperty(value = "下载状态: 0未下载 1已下载 2下载失败")
    private Integer downloadStatus;

    @ApiModelProperty(value = "飞书应用ID")
    private String appId;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}
