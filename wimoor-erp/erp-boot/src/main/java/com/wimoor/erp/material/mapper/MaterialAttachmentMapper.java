package com.wimoor.erp.material.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.erp.material.pojo.entity.MaterialAttachment;

@Mapper
public interface MaterialAttachmentMapper extends BaseMapper<MaterialAttachment> {

    List<MaterialAttachment> selectByMaterialId(@Param("materialid") String materialid);

    int deleteByIdAndShopid(@Param("id") String id, @Param("shopid") String shopid);
}
