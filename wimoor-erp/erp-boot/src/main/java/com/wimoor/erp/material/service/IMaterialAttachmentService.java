package com.wimoor.erp.material.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.material.pojo.entity.MaterialAttachment;

public interface IMaterialAttachmentService extends IService<MaterialAttachment> {

    List<MaterialAttachment> getByMaterialId(String materialid);

    void saveAttachments(String materialid, List<MaterialAttachment> attachments, UserInfo user);

    void deleteAttachment(String id, String shopid);
}
