package com.wimoor.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.admin.entity.SysHelpDoc;
import com.wimoor.admin.mapper.SysHelpDocMapper;
import com.wimoor.admin.service.ISysHelpDocService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 帮助文档Service实现类
 */
@Service
public class SysHelpDocServiceImpl extends ServiceImpl<SysHelpDocMapper, SysHelpDoc> implements ISysHelpDocService {

    @Override
    public List<SysHelpDoc> searchByKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return listAllForIndex();
        }
        return baseMapper.searchByKeyword(keyword);
    }

    @Override
    public List<SysHelpDoc> listByCategory(String category) {
        return baseMapper.listByCategory(category);
    }

    @Override
    public SysHelpDoc getByDocKey(String docKey) {
        return baseMapper.getByDocKey(docKey);
    }

    @Override
    public SysHelpDoc getByPath(String path) {
        return baseMapper.getByPath(path);
    }

    @Override
    public String getDocIndex() {
        List<SysHelpDoc> docs = baseMapper.listAllForIndex();
        JSONArray result = new JSONArray();
        
        // 按分类分组
        docs.stream()
            .collect(Collectors.groupingBy(SysHelpDoc::getCategory))
            .forEach((category, categoryDocs) -> {
                JSONObject categoryObj = new JSONObject();
                categoryObj.put("category", category);
                categoryObj.put("categoryName", getCategoryName(category));
                
                JSONArray docsArray = new JSONArray();
                for (SysHelpDoc doc : categoryDocs) {
                    JSONObject docObj = new JSONObject();
                    docObj.put("docKey", doc.getDocKey());
                    docObj.put("title", doc.getTitle());
                    docObj.put("keywords", doc.getKeywords());
                    docObj.put("relatedDocs", doc.getRelatedDocs());
                    docsArray.add(docObj);
                }
                categoryObj.put("docs", docsArray);
                result.add(categoryObj);
            });
        
        return result.toJSONString();
    }

    @Override
    public String searchRelevantDocs(String query, int limit) {
        if (StrUtil.isBlank(query)) {
            return "[]";
        }
        
        // 将查询拆分为关键词
        String[] keywords = query.split("[\\s,，。、]+");
        
        // 搜索匹配的文档
        List<SysHelpDoc> allDocs = baseMapper.listAllForIndex();
        List<SysHelpDoc> matchedDocs = allDocs.stream()
            .filter(doc -> {
                String searchText = (doc.getTitle() + " " + doc.getKeywords() + " " + doc.getDocKey()).toLowerCase();
                for (String keyword : keywords) {
                    if (searchText.contains(keyword.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            })
            .limit(limit)
            .collect(Collectors.toList());
        
        // 获取匹配文档的完整内容
        JSONArray result = new JSONArray();
        for (SysHelpDoc doc : matchedDocs) {
            SysHelpDoc fullDoc = baseMapper.getByDocKey(doc.getDocKey());
            if (fullDoc != null) {
                JSONObject docObj = new JSONObject();
                docObj.put("docKey", fullDoc.getDocKey());
                docObj.put("title", fullDoc.getTitle());
                docObj.put("category", fullDoc.getCategory());
                docObj.put("content", fullDoc.getContent());
                docObj.put("relatedDocs", fullDoc.getRelatedDocs());
                result.add(docObj);
            }
        }
        
        return result.toJSONString();
    }

    private List<SysHelpDoc> listAllForIndex() {
        return baseMapper.listAllForIndex();
    }

    private String getCategoryName(String category) {
        switch (category) {
            case "erp":
                return "ERP模块";
            case "amazon":
                return "Amazon模块";
            case "finance":
                return "财务模块";
            case "setting":
                return "设置模块";
            default:
                return category;
        }
    }
}
