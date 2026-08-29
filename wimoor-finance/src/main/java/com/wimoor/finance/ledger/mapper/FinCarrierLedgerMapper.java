package com.wimoor.finance.ledger.mapper;

import com.wimoor.finance.ledger.domain.FinCarrierReconcileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 承运商台账Mapper接口
 *
 * @author wimoor
 */
@Mapper
public interface FinCarrierLedgerMapper {

    /**
     * 查询承运商台账汇总（按承运商维度）
     */
    List<Map<String, Object>> selectCarrierLedgerSummary(@Param("shopid") String shopid,
                                                          @Param("groupid") String groupid,
                                                          @Param("marketplaceid") String marketplaceid,
                                                          @Param("companyid") String companyid,
                                                          @Param("channelid") String channelid,
                                                          @Param("fromDate") String fromDate,
                                                          @Param("toDate") String toDate);

    /**
     * 查询承运商台账统计数据（全局汇总）
     */
    Map<String, Object> selectCarrierLedgerStatistics(@Param("shopid") String shopid,
                                                       @Param("groupid") String groupid,
                                                       @Param("marketplaceid") String marketplaceid,
                                                       @Param("companyid") String companyid,
                                                       @Param("channelid") String channelid,
                                                       @Param("fromDate") String fromDate,
                                                       @Param("toDate") String toDate);

    /**
     * 查询承运商对账记录
     */
    FinCarrierReconcileRecord selectReconcileRecord(@Param("groupid") String groupid,
                                                     @Param("carrierId") String carrierId,
                                                     @Param("reconcileMonth") String reconcileMonth);

    /**
     * 查询承运商最新对账记录
     */
    FinCarrierReconcileRecord selectLatestReconcileRecord(@Param("groupid") String groupid,
                                                           @Param("carrierId") String carrierId);

    /**
     * 插入承运商对账记录
     */
    int insertReconcileRecord(FinCarrierReconcileRecord record);

    /**
     * 更新承运商对账记录
     */
    int updateReconcileRecord(FinCarrierReconcileRecord record);
}
