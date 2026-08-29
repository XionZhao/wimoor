package com.wimoor.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.admin.entity.SysHelpDoc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 帮助文档Mapper接口
 */
@Mapper
public interface SysHelpDocMapper extends BaseMapper<SysHelpDoc> {

    /**
     * 根据关键词搜索帮助文档
     * @param keyword 关键词
     * @return 匹配的帮助文档列表
     */
    @Select("SELECT * FROM t_sys_help_doc WHERE status = 1 AND (keywords LIKE CONCAT('%', #{keyword}, '%') OR title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%')) ORDER BY sort_order")
    List<SysHelpDoc> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据分类获取帮助文档列表
     * @param category 分类
     * @return 帮助文档列表
     */
    @Select("SELECT * FROM t_sys_help_doc WHERE status = 1 AND category = #{category} ORDER BY sort_order")
    List<SysHelpDoc> listByCategory(@Param("category") String category);

    /**
     * 根据docKey获取帮助文档
     * @param docKey 文档标识
     * @return 帮助文档
     */
    @Select("SELECT * FROM t_sys_help_doc WHERE status = 1 AND doc_key = #{docKey}")
    SysHelpDoc getByDocKey(@Param("docKey") String docKey);

    /**
     * 根据页面路径获取帮助文档
     * @param path 页面路径
     * @return 帮助文档
     */
    @Select("SELECT * FROM t_sys_help_doc WHERE status = 1 AND path = #{path}")
    SysHelpDoc getByPath(@Param("path") String path);

    /**
     * 获取所有启用的帮助文档（用于构建文档索引）
     * @return 帮助文档列表（只包含id、docKey、title、category、keywords字段）
     */
    @Select("SELECT id, doc_key, title, category, keywords, related_docs FROM t_sys_help_doc WHERE status = 1 ORDER BY category, sort_order")
    List<SysHelpDoc> listAllForIndex();
}
