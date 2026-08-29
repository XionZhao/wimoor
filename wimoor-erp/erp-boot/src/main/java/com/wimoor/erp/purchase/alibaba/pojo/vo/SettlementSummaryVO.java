package com.wimoor.erp.purchase.alibaba.pojo.vo;

import java.math.BigDecimal;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 1688账期结算汇总VO
 */
@Data
@ApiModel(value="SettlementSummaryVO对象", description="1688账期结算汇总")
public class SettlementSummaryVO {

    @ApiModelProperty(value = "已结转订单笔数")
    private Integer settledCount = 0;

    @ApiModelProperty(value = "已结转入账金额")
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @ApiModelProperty(value = "已结转已付金额")
    private BigDecimal settledPaid = BigDecimal.ZERO;

    @ApiModelProperty(value = "未结转订单笔数")
    private Integer unsettledCount = 0;

    @ApiModelProperty(value = "未结转订单金额")
    private BigDecimal unsettledAmount = BigDecimal.ZERO;

    @ApiModelProperty(value = "未结转未付金额")
    private BigDecimal unsettledUnpaid = BigDecimal.ZERO;
}
