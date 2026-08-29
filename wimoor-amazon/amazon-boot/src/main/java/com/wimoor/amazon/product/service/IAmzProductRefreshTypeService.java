package com.wimoor.amazon.product.service;

import com.wimoor.amazon.product.pojo.dto.ProductAsyncInitDTO;
import com.wimoor.amazon.product.pojo.entity.AmzProductRefreshType;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 产品刷新类型服务接口
 * 替代原IAmzProductRefreshService
 *
 * @author wimoor team
 * @since 2022-06-17
 */
public interface IAmzProductRefreshTypeService extends IService<AmzProductRefreshType> {

    /**
     * 插入默认刷新记录
     */
    void insert();

    /**
     * 查询待刷新详情的记录
     */
    AmzProductRefreshType findForDetailRefresh(String amazonauthid);

    /**
     * 查询待刷新目录的记录列表
     */
    AmzProductRefreshType findForCatalogRefresh(String amazonauthid);

    /**
     * 查询待刷新价格的记录列表
     */
    List<AmzProductRefreshType> findForPriceRefresh(String amazonauthid);

    /**
     * 同步产品信息，重置刷新时间
     */
    Integer syncProductInfo(ProductAsyncInitDTO dto);

    /**
     * 根据pid和type查询刷新记录
     */
    AmzProductRefreshType getByPidAndType(String pid, Integer type);

    /**
     * 根据pid和type更新刷新时间
     */
    boolean updateRefreshTime(String pid, Integer type, java.time.LocalDateTime refreshTime);
}
