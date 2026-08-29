package com.wimoor.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wimoor.admin.entity.SysHelpDoc;
import java.util.List;

/**
 * 帮助文档Service接口
 */
public interface ISysHelpDocService extends IService<SysHelpDoc> {

    /**
     * 根据关键词搜索帮助文档
     * @param keyword 关键词
     * @return 匹配的帮助文档列表
     */
    List<SysHelpDoc> searchByKeyword(String keyword);

    /**
     * 根据分类获取帮助文档列表
     * @param category 分类
     * @return 帮助文档列表
     */
    List<SysHelpDoc> listByCategory(String category);

    /**
     * 根据docKey获取帮助文档
     * @param docKey 文档标识
     * @return 帮助文档
     */
    SysHelpDoc getByDocKey(String docKey);

    /**
     * 根据页面路径获取帮助文档
     * @param path 页面路径
     * @return 帮助文档
     */
    SysHelpDoc getByPath(String path);

    /**
     * 获取所有帮助文档的索引（用于AI工具）
     * @return 文档索引JSON字符串
     */
    String getDocIndex();

    /**
     * 根据用户问题智能搜索相关帮助文档
     * @param query 用户问题
     * @param limit 返回数量限制
     * @return 相关文档内容
     */
    String searchRelevantDocs(String query, int limit);
}
