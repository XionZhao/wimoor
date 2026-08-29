package com.wimoor.finance.ledger.mapper;

import com.wimoor.finance.ledger.domain.FinInvoiceExtension;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 发票扩展信息Mapper接口
 */
@Mapper
public interface FinInvoiceExtensionMapper {

    /** 根据发票ID查询扩展信息 */
    List<FinInvoiceExtension> selectByInvoiceId(Long invoiceId);

    /** 批量插入扩展信息 */
    int batchInsert(List<FinInvoiceExtension> list);

    /** 删除指定发票的扩展信息 */
    int deleteByInvoiceId(Long invoiceId);
}
