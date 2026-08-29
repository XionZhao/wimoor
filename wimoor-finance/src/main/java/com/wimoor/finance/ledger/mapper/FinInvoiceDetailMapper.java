package com.wimoor.finance.ledger.mapper;

import com.wimoor.finance.ledger.domain.FinInvoiceDetail;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 发票明细行Mapper接口
 */
@Mapper
public interface FinInvoiceDetailMapper {

    /** 根据发票ID查询明细行 */
    List<FinInvoiceDetail> selectByInvoiceId(Long invoiceId);

    /** 批量插入明细行 */
    int batchInsert(List<FinInvoiceDetail> list);

    /** 删除指定发票的明细行 */
    int deleteByInvoiceId(Long invoiceId);
}
