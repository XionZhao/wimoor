package com.wimoor.amazon.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.amazon.spapi.SellingPartnerAPIAA.LWAException;
import com.amazon.spapi.api.ProductPricingApi;
import com.amazon.spapi.client.ApiCallback;
import com.amazon.spapi.client.ApiException;
import com.amazon.spapi.model.productpricing.*;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.service.IAmazonAuthorityService;
import com.wimoor.amazon.auth.service.IMarketplaceService;
import com.wimoor.amazon.auth.service.impl.ApiBuildService;
import com.wimoor.amazon.notifications.service.IAwsSQSMessageHandlerService;
import com.wimoor.amazon.product.mapper.ProductInOptMapper;
import com.wimoor.amazon.product.mapper.ProductInOrderMapper;
import com.wimoor.amazon.product.mapper.ProductPriceMapper;
import com.wimoor.amazon.product.pojo.entity.*;
import com.wimoor.amazon.product.service.*;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.service.IPictureService;
import okhttp3.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service("productProductPriceService")
public class ProductProductPriceServiceImpl implements IProductProductPriceService, IAwsSQSMessageHandlerService {
	private static final Logger log = LoggerFactory.getLogger(ProductProductPriceServiceImpl.class);
	@Resource
	private IProductInfoService iProductInfoService;
	@Resource
	private ProductInOptMapper productInOptMapper;
	@Autowired
	ApiBuildService apiBuildService;
	@Autowired
	IAmzProductRefreshTypeService iAmzProductRefreshTypeService;
	@Autowired
	IPictureService pictureService;
	@Autowired
	IProductCatalogItemService iProductCaptureCatalogItemService;
    @Autowired
    IMarketplaceService marketplaceService;
    @Autowired
	private ProductPriceMapper productPriceMapper;
	@Autowired
	IAmazonAuthorityService amazonAuthorityService;
	@Autowired
	IProductFollowHandlerService iProductFollowHandlerService;
	@Resource
	ProductInOrderMapper productInOrderMapper;
	@Override
	public GetPricingResponse captureProductPrice(AmazonAuthority amazonAuthority, String sku, String  marketplaceid) {
		// TODO Auto-generated method stub
		  ProductPricingApi api = apiBuildService.getProductPricingApi(amazonAuthority);
		try {
			 GetPricingResponse response = api.getPricing(marketplaceid,"Sku", null, Arrays.asList(sku), null,null);
			return response;
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("接口调用错误："+e.getMessage());
		} catch (LWAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("接口调用错误："+e.getMessage());
		}
	}
	  
	  
	@Override
	public void runApi(AmazonAuthority amazonAuthority) {
		List<AmzProductRefreshType> skuRefreshlist = iAmzProductRefreshTypeService.findForPriceRefresh(amazonAuthority.getId());
		if (skuRefreshlist == null || skuRefreshlist.isEmpty()) return;
		// 批量查询产品信息，补充sku、marketplaceid
		List<String> pids = skuRefreshlist.stream()
				.map(s -> s.getPid().toString())
				.collect(Collectors.toList());
		List<ProductInfo> products = iProductInfoService.listByIds(pids);
		Map<String, ProductInfo> productMap = products.stream()
				.collect(Collectors.toMap(ProductInfo::getId, p -> p, (a, b) -> a));
		for (AmzProductRefreshType item : skuRefreshlist) {
			ProductInfo info = productMap.get(item.getPid().toString());
			if (info == null) continue;
			item.setSku(info.getSku());
			item.setMarketplaceid(info.getMarketplaceid());
		}
		// 过滤掉查不到产品信息的记录
		skuRefreshlist = skuRefreshlist.stream()
				.filter(s -> s.getSku() != null && s.getMarketplaceid() != null)
				.collect(Collectors.toList());
		if (skuRefreshlist.isEmpty()) return;
		// 按 marketplaceid 分组，循环处理每组
		Map<String, List<AmzProductRefreshType>> groupByMarket = skuRefreshlist.stream()
				.collect(Collectors.groupingBy(AmzProductRefreshType::getMarketplaceid));
		for (Map.Entry<String, List<AmzProductRefreshType>> entry : groupByMarket.entrySet()) {
			String marketplaceid = entry.getKey();
			List<AmzProductRefreshType> groupItems = entry.getValue();
			List<String> skulist = groupItems.stream()
					.map(AmzProductRefreshType::getSku)
					.collect(Collectors.toList());
			AmzProductRefreshType skuRefresh = groupItems.get(0);
			skuRefresh.setMarketplaceid(marketplaceid);
			skuRefresh.setSku(String.join(",", skulist));
			captureProductPriceSync(amazonAuthority, skuRefresh, skulist);
		}
	}
	
	@Override
	public Call captureProductPriceSync(AmazonAuthority amazonAuthority, AmzProductRefreshType amzProductRefresh,List<String> skulist) {
		// TODO Auto-generated method stub
		  amazonAuthority.setUseApi("getPricing");
		  ProductPricingApi api = apiBuildService.getProductPricingApi(amazonAuthority);
		try {
			if(amazonAuthority.apiNotRateLimit()) {
				ApiCallback<GetPricingResponse> callback =new  ApiCallbackGetPricing(this,amazonAuthority,amzProductRefresh);
				Call item = api.getPricingAsync(amzProductRefresh.getMarketplaceid(),"Sku", null, skulist, null,null, callback);
				return item;
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			amazonAuthority.setApiRateLimit(null, e);
		} catch (LWAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			amazonAuthority.setApiStatus(e);
		}
		return null;
	}

 
public GetOffersResponse getItemOffers(AmazonAuthority amazonAuthority, String asin, String  marketplaceid) {
		ProductPricingApi api = apiBuildService.getProductPricingApi(amazonAuthority);
		try {
			   GetOffersResponse response = api.getItemOffers(marketplaceid,"New", asin,null);
			return response;
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("接口调用错误："+e.getMessage());
		} catch (LWAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("接口调用错误："+e.getMessage());
		}
}
	 
public void handleResultItemOffers(AmazonAuthority amazonAuthority, String asin, String  marketplaceid, GetOffersResponse response) {
	  GetOffersResult result = response.getPayload();
	  OfferDetailList offers = result.getOffers();
	  iProductFollowHandlerService.recordFollowOfferChange(offers, asin, marketplaceid);
}
 


		@Override
		public void handlerResult(GetPricingResponse result, AmazonAuthority amazonAuthority,
				String marketplaceid) {
			// TODO Auto-generated method stub
			PriceList priceList = result.getPayload();
			for(Price productPrice:priceList) {
				String sku = productPrice.getSellerSKU();
				String amazonauthid=amazonAuthority.getId();
				List<ProductInfo> infolist = iProductInfoService.selectBySku(sku, marketplaceid, amazonauthid);
				if(infolist!=null&&infolist.size()>0) {
					ProductInfo info=infolist.get(0);
					Product product = productPrice.getProduct();
					String pid = info.getId();
					if(product==null) {
						iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_PRICE, LocalDateTime.now());
						continue; 
					}
					OffersList offerlist = product.getOffers();
					if(offerlist==null) {
						iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_PRICE, LocalDateTime.now());
						List<ProductPrice> list = productPriceMapper.findbyProductID(info.getId());
						if(list!=null&&list.size()>0) {
							//for根据byday日期最新 找出这个oldprice
							 ProductPrice oldprice = list.stream().max(Comparator.comparing(ProductPrice::getByday)).get();
							if(oldprice!=null&&(oldprice.getLandedAmount()!=info.getPrice())){
								info.setPrice(oldprice.getLandedAmount());
								iProductInfoService.updateById(info);
								ProductInOpt productInOpt = productInOptMapper.selectById(info.getId());
								if(productInOpt!=null) {
									productInOpt.setBuyprice(oldprice.getLandedAmount());
									productInOpt.setLastupdate(new Date());
									productInOptMapper.updateById(productInOpt);
								}
							}
						}
						continue;
					}
					if(offerlist!=null) {
						// ASIN长度校验：不能超过10位
						String asin = info.getAsin();
						if(asin != null && asin.length() > 10) {
							log.warn("ASIN长度超过10位，跳过处理: pid={}, asin={}", pid, asin);
							continue;
						}
						for(OfferType offer:offerlist) {
							ProductPrice price = new ProductPrice();
							price.setAsin(asin);
							price.setMarketplaceid(marketplaceid);
							price.setByday(new Date());
							price.setIsnewest(true);
							price.setFulfillmentchannel(offer.getFulfillmentChannel());
							price.setItemcondition(offer.getItemCondition());
							price.setItemsubcondition(offer.getItemSubCondition());
							price.setPtype(ProductPriceType.BuyPrice);
							PriceType buyPrice = offer.getBuyingPrice();
							if (buyPrice == null||
									(!offer.getItemCondition().equals("New"))||
									(offer.getOfferType()!=null&&offer.getOfferType().equals(OfferCustomerType.B2B))) {
								continue;
							}
							price.setListingAmount(buyPrice.getListingPrice().getAmount());
							price.setListingCurrency(buyPrice.getListingPrice().getCurrencyCode());
							price.setLandedAmount(buyPrice.getLandedPrice().getAmount());
							price.setLandedCurrency(buyPrice.getLandedPrice().getCurrencyCode());
							price.setShippingAmount(buyPrice.getShipping().getAmount());
							price.setShippingCurrency(buyPrice.getShipping().getCurrencyCode());
							price.setSellersku(offer.getSellerSKU());
							price.setSellerid(amazonAuthority.getSellerid());
							productPriceMapper.insert(price);
							ProductInOpt productInOpt = productInOptMapper.selectById(info.getId());
							info.setPrice(buyPrice.getLandedPrice().getAmount());
							iProductInfoService.updateById(info);
								if (productInOpt == null) {
									productInOpt = new ProductInOpt();
									productInOpt.setPid(new BigInteger(info.getId()));
									productInOpt.setLastupdate(new Date());
									productInOpt.setBuyprice(buyPrice.getLandedPrice().getAmount());
									productInOptMapper.insert(productInOpt);
								} else {
									productInOpt.setBuyprice(buyPrice.getLandedPrice().getAmount());
									productInOpt.setLastupdate(new Date());
									productInOptMapper.updateById(productInOpt);
								}
			      }
			}
		    iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_PRICE, LocalDateTime.now());
			}
		}
	
		}
	
		@Override
		public void handlerFailure(AmazonAuthority auth, AmzProductRefreshType skuRefresh, ApiException e) {
			// TODO Auto-generated method stub
			if(skuRefresh!=null&&skuRefresh.getPid()!=null) {
				String pid = skuRefresh.getPid().toString();
				if(e.getMessage().contains("Not Found")) {
					ProductInOrder order = productInOrderMapper.selectById(pid);
					if(order!=null&&order.getSalesMonth()!=null&&order.getSalesMonth()>0) {
						iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_PRICE, LocalDateTime.now());
					}else {
						iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_PRICE, LocalDateTime.now());
						ProductInfo info = iProductInfoService.getById(pid);
						info.setInvalid(true);
						iProductInfoService.updateById(info);
					}
				}else {
					iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_PRICE, LocalDateTime.now());
				}
			}
		}

        boolean isrun=true;
		@Override
		public void runTask() {
			// TODO Auto-generated method stub
			ProductProductPriceServiceImpl self=this;
		    new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while(isrun) {
						try {
							amazonAuthorityService.executTask(self);
							Thread.sleep(20000);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
		    }).start();
			
		}


		@Override
		public void stopTask() {
			// TODO Auto-generated method stub
			isrun=false;
		}

	@Override
	public boolean handlerMessage(JSONObject body) {
          System.out.println(body.toJSONString());
		return false;
	}
}
