package com.wimoor.amazon.finances.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wimoor.amazon.finances.pojo.entity.TransactionReport;
import com.wimoor.amazon.finances.service.TransactionReportService;
import com.wimoor.amazon.finances.mapper.TransactionReportMapper;
import org.springframework.stereotype.Service;

/**
* @author liufei
* @description 针对表【t_amz_transaction_report(亚马逊交易报告表)】的数据库操作Service实现
* @createDate 2026-07-20 10:00:00
*/
@Service
public class TransactionReportServiceImpl extends ServiceImpl<TransactionReportMapper, TransactionReport>
    implements TransactionReportService{

}
