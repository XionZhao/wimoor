package com.wimoor.amazon.product.mapper;

import com.wimoor.amazon.product.pojo.dto.ProductAsyncInitDTO;
import com.wimoor.amazon.product.pojo.entity.AmzProductRefreshType;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 产品刷新类型Mapper接口
 * 替代原AmzProductRefreshMapper
 *
 * @author wimoor team
 * @since 2022-06-17
 */
@Mapper
public interface AmzProductRefreshTypeMapper extends BaseMapper<AmzProductRefreshType> {

    /**
     * 插入默认刷新记录
     */
    void insertDefault();

    /**
     * 查询待刷新详情的记录
     */
    AmzProductRefreshType findForDetailRefresh(@Param("amazonauthid") String amazonauthid);

    /**
     * 查询待刷新目录的记录列表
     */
    AmzProductRefreshType findForCatalogRefresh(@Param("amazonauthid") String amazonauthid);

    /**
     * 查询待刷新价格的记录列表
     */
    List<AmzProductRefreshType> findForPriceRefresh(@Param("amazonauthid") String amazonauthid);

    /**
     * 同步产品信息，重置刷新时间
     */
    Integer syncProductInfo(@Param("dto") ProductAsyncInitDTO dto);
}
