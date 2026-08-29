package com.wimoor.erp.finance.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wimoor.erp.finance.pojo.entity.FinAccountPeriodRollover;

/**
 * 账期末结转记录Mapper
 */
@Mapper
public interface FinAccountPeriodRolloverMapper extends BaseMapper<FinAccountPeriodRollover> {

    /**
     * 分页查询结转记录列表
     */
    IPage<Map<String, Object>> selectRolloverList(Page<?> page, @Param("params") Map<String, Object> params);

    /**
     * 查询结转详情（关联的付款明细）
     */
    List<Map<String, Object>> selectRolloverDetail(@Param("rolloverId") String rolloverId);
}
