package com.wimoor.finance.api;

import com.wimoor.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;


@Component
@FeignClient(value = "wimoor-amazon")
public interface RemoteAmazonService
{
    @GetMapping("/amazon/api/v1/amzgroup/list")
    public Result<List<Map<String,Object>>> getAmazonGroupAction();

    @GetMapping("/amazon/api/v1/amzmarketplace/getByName")
    public Result<?> getMarketplaceAction(@RequestParam("name")String name) ;

    @GetMapping("/amazon/api/v1/amzmarketplace/getByCountry")
    public Result<?> getMarketplaceByCountryAction(@RequestParam("country")String country);


    @PostMapping("/amazon/api/v1/settlement/getMonthReport")
    public Result<?> getMonthReportAction(@RequestBody Map<String,Object> param) ;

    @GetMapping("/amazon/api/v1/exchangeRate/getMyCurrencyRate")
    public Result<List<Map<String, Object>>> getMyCurrencyRate(@RequestParam("byday") String byday);
}
