package com.wimoor.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.wimoor.admin.entity.SysHelpDoc;
import com.wimoor.admin.service.ISysHelpDocService;
import com.wimoor.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 帮助文档Controller
 */
@Api(tags = "帮助文档接口")
@RestController
@RequestMapping("/api/v1/help-doc")
@RequiredArgsConstructor
public class SysHelpDocController {

    private final ISysHelpDocService sysHelpDocService;

    @ApiOperation("搜索帮助文档")
    @GetMapping("/search")
    public Result<?> search(
            @ApiParam("搜索关键词") @RequestParam(required = false) String keyword,
            @ApiParam("文档分类") @RequestParam(required = false) String category,
            @ApiParam("返回数量限制") @RequestParam(required = false, defaultValue = "3") Integer limit) {
        
        List<SysHelpDoc> docs;
        if (StrUtil.isNotBlank(category)) {
            docs = sysHelpDocService.listByCategory(category);
        } else {
            docs = sysHelpDocService.searchByKeyword(keyword);
        }
        
        // 限制返回数量
        if (limit != null && limit > 0 && docs.size() > limit) {
            docs = docs.subList(0, limit);
        }
        
        return Result.success(docs);
    }

    @ApiOperation("获取帮助文档详情")
    @GetMapping("/get")
    public Result<?> get(
            @ApiParam("文档标识") @RequestParam String docKey) {
        
        if (StrUtil.isBlank(docKey)) {
            return Result.failed("文档标识不能为空");
        }
        
        SysHelpDoc doc = sysHelpDocService.getByDocKey(docKey);
        if (doc == null) {
            return Result.failed("未找到对应的帮助文档");
        }
        
        return Result.success(doc);
    }

    @ApiOperation("根据页面路径获取帮助文档")
    @GetMapping("/get-by-path")
    public Result<?> getByPath(
            @ApiParam("页面路径") @RequestParam String path) {
        
        if (StrUtil.isBlank(path)) {
            return Result.success(null);
        }
        
        // 清理路径，移除hash和查询参数
        String cleanPath = path;
        if (cleanPath.contains("#")) {
            cleanPath = cleanPath.substring(cleanPath.indexOf("#") + 1);
        }
        if (cleanPath.contains("?")) {
            cleanPath = cleanPath.substring(0, cleanPath.indexOf("?"));
        }
        
        SysHelpDoc doc = sysHelpDocService.getByPath(cleanPath);
        if (doc == null) {
            // 如果精确匹配失败，尝试模糊匹配
            List<SysHelpDoc> allDocs = sysHelpDocService.searchByKeyword(cleanPath);
            if (!allDocs.isEmpty()) {
                doc = allDocs.get(0);
            }
        }
        
        // 如果没有找到帮助文档，返回空数据而不是错误
        return Result.success(doc);
    }

    @ApiOperation("获取帮助文档索引")
    @GetMapping("/index")
    public Result<?> index() {
        String indexJson = sysHelpDocService.getDocIndex();
        return Result.success(indexJson);
    }
}
