package com.wimoor.erp.purchase.pojo.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ManualPaymentSaveDTO对象", description="手动新增请款单")
public class ManualPaymentSaveDTO {

	String groupid;

	String supplierid;

	String feelist;

	String paymethod;

	String payacc;

	String remark;
}
