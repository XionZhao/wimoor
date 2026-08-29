package com.wimoor.amazon.finances.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.amazon.finances.pojo.entity.AmzTransactionReportColumnMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AmzTransactionReportColumnMappingMapper extends BaseMapper<AmzTransactionReportColumnMapping> {

    List<AmzTransactionReportColumnMapping> selectByMarketplaceId(@Param("marketplaceid") String marketplaceid);
}