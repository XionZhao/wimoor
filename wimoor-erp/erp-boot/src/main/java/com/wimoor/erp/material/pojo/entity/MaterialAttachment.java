package com.wimoor.erp.material.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wimoor.erp.common.pojo.entity.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "MaterialAttachment对象", description = "物料附件表")
@TableName("t_erp_material_attachment")
public class MaterialAttachment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "物料ID")
    @TableField(value = "materialid")
    private String materialid;

    @ApiModelProperty(value = "店铺ID")
    @TableField(value = "shopid")
    private String shopid;

    @ApiModelProperty(value = "文件ID")
    @TableField(value = "fileid")
    private String fileid;

    @ApiModelProperty(value = "文件名称")
    @TableField(value = "file_name")
    private String fileName;

    @ApiModelProperty(value = "文件类型")
    @TableField(value = "file_type")
    private String fileType;

    @ApiModelProperty(value = "文件路径")
    @TableField(value = "file_path")
    private String filePath;

    @ApiModelProperty(value = "操作时间")
    @TableField(value = "opttime")
    private Date opttime;
}
