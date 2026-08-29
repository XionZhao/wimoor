package com.wimoor.finance.setting.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP凭证策略工厂
 *
 * @author wimoor
 * @date 2026-08-11
 */
@Component
public class ErpVoucherStrategyFactory {

    @Autowired
    private List<IErpVoucherStrategy> strategyList;

    private final Map<String, IErpVoucherStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (IErpVoucherStrategy strategy : strategyList) {
            strategyMap.put(strategy.getFtype(), strategy);
        }
    }

    public IErpVoucherStrategy getStrategy(String ftype) {
        IErpVoucherStrategy strategy = strategyMap.get(ftype);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到ERP凭证类型为 [" + ftype + "] 的处理策略");
        }
        return strategy;
    }

    public boolean hasStrategy(String ftype) {
        return strategyMap.containsKey(ftype);
    }
}