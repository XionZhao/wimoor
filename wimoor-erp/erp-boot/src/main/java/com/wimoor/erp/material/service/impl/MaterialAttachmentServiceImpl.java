package com.wimoor.erp.material.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.admin.api.AdminClientOneFeign;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.material.mapper.MaterialAttachmentMapper;
import com.wimoor.erp.material.pojo.entity.MaterialAttachment;
import com.wimoor.erp.material.service.IMaterialAttachmentService;

@Service
public class MaterialAttachmentServiceImpl extends ServiceImpl<MaterialAttachmentMapper, MaterialAttachment>
        implements IMaterialAttachmentService {

    @Autowired
    AdminClientOneFeign adminClientOneFeign;

    @Override
    public List<MaterialAttachment> getByMaterialId(String materialid) {
        return baseMapper.selectByMaterialId(materialid);
    }

    @Override
    @Transactional
    public void saveAttachments(String materialid, List<MaterialAttachment> attachments, UserInfo user) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (MaterialAttachment attachment : attachments) {
            attachment.setMaterialid(materialid);
            attachment.setShopid(user.getCompanyid());
            attachment.setOpttime(new Date());
            if (attachment.idIsNULL()) {
                this.save(attachment);
            }
        }
    }

    @Override
    @Transactional
    public void deleteAttachment(String id, String shopid) {
        // 先查询附件记录获取文件路径
        MaterialAttachment attachment = this.getById(id);
        if (attachment != null && attachment.getShopid().equals(shopid)) {
            // 调用admin服务删除文件源和t_sys_tool_large_file记录
            if (attachment.getFilePath() != null) {
                try {
                    adminClientOneFeign.deleteFile("material", attachment.getFilePath());
                } catch (Exception e) {
                    // 文件删除失败不影响附件记录删除
                    e.printStackTrace();
                }
            }
            // 最后删除t_erp_material_attachment记录
            baseMapper.deleteByIdAndShopid(id, shopid);
        }
    }
}
