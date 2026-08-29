package com.wimoor.amazon.product.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import com.wimoor.amazon.product.pojo.dto.ProductAsyncInitDTO;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.amazon.product.mapper.AmzProductRefreshTypeMapper;
import com.wimoor.amazon.product.pojo.entity.AmzProductRefreshType;
import com.wimoor.amazon.product.service.IAmzProductRefreshTypeService;

import lombok.RequiredArgsConstructor;

/**
 * 产品刷新类型服务实现类
 * 替代原AmzProductRefreshServiceImpl
 *
 * @author wimoor team
 * @since 2022-06-17
 */
@Service
@RequiredArgsConstructor
public class AmzProductRefreshTypeServiceImpl extends ServiceImpl<AmzProductRefreshTypeMapper, AmzProductRefreshType> implements IAmzProductRefreshTypeService {

    @Override
    public void insert() {
        this.baseMapper.insertDefault();
    }

    @Override
    public AmzProductRefreshType findForDetailRefresh(String amazonauthid) {
        return this.baseMapper.findForDetailRefresh(amazonauthid);
    }

    @Override
    public AmzProductRefreshType findForCatalogRefresh(String amazonauthid) {
        return this.baseMapper.findForCatalogRefresh(amazonauthid);
    }

    @Override
    public List<AmzProductRefreshType> findForPriceRefresh(String amazonauthid) {
        return this.baseMapper.findForPriceRefresh(amazonauthid);
    }

    @Override
    public Integer syncProductInfo(ProductAsyncInitDTO dto) {
        return this.baseMapper.syncProductInfo(dto);
    }

    @Override
    public AmzProductRefreshType getByPidAndType(String pid, Integer type) {
        return this.getOne(new LambdaQueryWrapper<AmzProductRefreshType>()
                .eq(AmzProductRefreshType::getPid, pid)
                .eq(AmzProductRefreshType::getType, type));
    }

    @Override
    public boolean updateRefreshTime(String pid, Integer type, LocalDateTime refreshTime) {
        LambdaUpdateWrapper<AmzProductRefreshType> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AmzProductRefreshType::getPid, pid)
               .eq(AmzProductRefreshType::getType, type)
               .set(AmzProductRefreshType::getRefreshTime, refreshTime);
        return this.update(wrapper);
    }
}
