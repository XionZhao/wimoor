package com.wimoor.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 帮助文档实体类
 */
@Data
@TableName("t_sys_help_doc")
public class SysHelpDoc implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档标识（如：采购单、库存盘点、发货计划等）
     */
    private String docKey;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容（Markdown格式）
     */
    private String content;

    /**
     * 分类（如：erp、amazon、finance、setting等）
     */
    private String category;

    /**
     * 关键词（逗号分隔，用于搜索）
     */
    private String keywords;

    /**
     * 页面路径（用于根据URL匹配帮助文档）
     */
    private String path;

    /**
     * 关联文档（逗号分隔的doc_key）
     */
    private String relatedDocs;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态（1：启用，0：禁用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
