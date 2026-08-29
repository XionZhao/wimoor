package com.wimoor.amazon.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.amazon.spapi.SellingPartnerAPIAA.LWAException;
import com.amazon.spapi.api.CatalogApi;
import com.amazon.spapi.client.ApiException;
import com.amazon.spapi.model.catalogitems.*;
import com.amazon.spapi.model.catalogitems.ItemImage.VariantEnum;
import com.wimoor.amazon.auth.pojo.entity.AmazonAuthority;
import com.wimoor.amazon.auth.pojo.entity.Marketplace;
import com.wimoor.amazon.auth.service.IAmazonAuthorityService;
import com.wimoor.amazon.auth.service.IMarketplaceService;
import com.wimoor.amazon.auth.service.impl.ApiBuildService;
import com.wimoor.amazon.common.mapper.DimensionsInfoMapper;
import com.wimoor.amazon.common.pojo.entity.DimensionsInfo;
import com.wimoor.amazon.product.mapper.ProductInOptMapper;
import com.wimoor.amazon.product.mapper.ProductInOrderMapper;
import com.wimoor.amazon.product.pojo.dto.ProductCatalogItemsDTO;
import com.wimoor.amazon.product.pojo.entity.AmzProductRefreshType;
import com.wimoor.amazon.product.pojo.entity.ProductInOrder;
import com.wimoor.amazon.product.pojo.entity.ProductInfo;
import com.wimoor.amazon.product.pojo.entity.ProductRank;
import com.wimoor.amazon.product.service.IAmzProductRefreshTypeService;
import com.wimoor.amazon.product.service.IProductCatalogItemService;
import com.wimoor.amazon.product.service.IProductInfoService;
import com.wimoor.amazon.product.service.IProductRankService;
import com.wimoor.common.GeneralUtil;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.pojo.entity.Picture;
import com.wimoor.common.service.IPictureService;
import com.wimoor.common.service.impl.PictureServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductCatalogItemServiceImpl implements IProductCatalogItemService {
	private static final Logger log = LoggerFactory.getLogger(ProductCatalogItemServiceImpl.class);
	@Resource
	private IProductInfoService iProductInfoService;
	@Resource
	private ProductInOrderMapper productInOrderMapper;
	@Resource
	private ProductInOptMapper productInOptMapper;
	@Autowired
	ApiBuildService apiBuildService;
	@Autowired
	IAmzProductRefreshTypeService iAmzProductRefreshTypeService;
	@Autowired
	IPictureService pictureService;
    @Autowired
    IProductRankService iProductRankService;
	@Autowired
	IAmazonAuthorityService amazonAuthorityService;
	@Autowired
	IMarketplaceService iMarketplaceService;
	@Autowired
	DimensionsInfoMapper dimensionsInfoMapper;
	public ItemSearchResults searchCatalogProducts(AmazonAuthority auth,ProductCatalogItemsDTO dto) {
		// TODO Auto-generated method stub
		auth.setUseApi("searchCatalogItems");
		try {
			if(auth.apiNotRateLimit()) {
				List<String> marketplaceIds=dto.getMarketplaceIds();
				List<String> identifiers=dto.getIdentifiers();
				String identifiersType=dto.getIdentifiersType();
				List<String> includedData=dto.getIncludedData()!=null?dto.getIncludedData():new ArrayList<String>();
				String locale=dto.getLocale();
				String sellerId=dto.getSellerId();
				List<String> keywords=dto.getKeywords();
				List<String> brandNames=dto.getBrandNames();
				List<String> classificationIds=dto.getClassificationIds();
				Integer pageSize=dto.getPageSize();
				String pageToken=dto.getPageToken();
				String keywordsLocale=dto.getKeywordsLocale();
				includedData.add("salesRanks");
				includedData.add("images");
				includedData.add("attributes");
				includedData.add("summaries");
				if(keywords!=null&&keywords.size()==1) {
					String asin=keywords.get(0);
					if(asin.indexOf("B")>=0&&asin.length()==10) {
						keywords=null;
						keywordsLocale=null;
						identifiers=Arrays.asList(asin);
						identifiersType="ASIN";
					}
				}
				CatalogApi api = apiBuildService.getCatalogApi(auth);
				ItemSearchResults response = api.searchCatalogItems(marketplaceIds,
						    identifiers,identifiersType,
						    includedData,locale,sellerId,
							keywords,brandNames,classificationIds,pageSize,pageToken,keywordsLocale);
			    return response;
			}else {
				throw new BizException("API申请频繁，请稍后重试");
			}
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			auth.setApiRateLimit( null, e);
		} catch (LWAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	
	@Override
	public void runApi(AmazonAuthority amazonAuthority) {
		AmzProductRefreshType skuRefresh = iAmzProductRefreshTypeService.findForCatalogRefresh(amazonAuthority.getId());
		if (skuRefresh == null) return;
		// 查询产品信息，补充sku、asin
		ProductInfo productInfo = iProductInfoService.getById(skuRefresh.getPid().toString());
		if (productInfo == null) return;
		skuRefresh.setSku(productInfo.getSku());
		skuRefresh.setAsin(productInfo.getAsin());
		if (skuRefresh.getAsin() == null || StrUtil.isBlank(skuRefresh.getAsin())) return;
		// 查询当前auth下的所有站点
		List<String> marketList = iMarketplaceService.findbyauth(amazonAuthority.getId())
				.stream().map(Marketplace::getMarketplaceid).collect(Collectors.toList());
		if (marketList.isEmpty()) return;
		skuRefresh.setMarketplaceid(String.join(",", marketList));
		captureCatalogProductSync(amazonAuthority, skuRefresh, marketList);
	}
	@Override
	public Item captureCatalogProductDim(AmazonAuthority auth,String asin, List<String> market) {
		// TODO Auto-generated method stub
		List<String> includedData=new ArrayList<String>();
		includedData.add("dimensions");
		includedData.add("attributes");
		return captureCatalogProduct( auth, asin, market, includedData) ;
	}

	@Override
	public Item captureCatalogProduct(AmazonAuthority auth,String asin, List<String> market, List<String> includedData) {
		// TODO Auto-generated method stub
		CatalogApi api = apiBuildService.getCatalogApi(auth);
		Item response =null;
		try {
			response = api.getCatalogItem(asin,market, includedData,null);
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("接口调用错误："+e.getMessage());
		} catch (LWAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}
	
	@Override
	public Item captureCatalogProduct(AmazonAuthority auth, AmzProductRefreshType skuRefresh, List<String> market) {
		// TODO Auto-generated method stub
		CatalogApi api = apiBuildService.getCatalogApi(auth);
		Item response =null;
		try {
			List<String> includedData=new ArrayList<String>();
			includedData.add("relationships");
			includedData.add("summaries");
			includedData.add("dimensions");
			response = api.getCatalogItem(skuRefresh.getAsin(),market, includedData,null);
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("接口调用错误："+e.getResponseBody());
		} catch (LWAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new BizException("授权错误");
		}
		return response;
	}

	@Override
	public void captureCatalogProductSync(AmazonAuthority auth, AmzProductRefreshType skuRefresh, List<String> market) {
		// TODO Auto-generated method stub
		auth.setUseApi("getCatalogItem");
		try {
			if(auth.apiNotRateLimit()) {
				ApiCallbackGetCatalogItem callback = new ApiCallbackGetCatalogItem(this,auth,skuRefresh);
				List<String> includedData=new ArrayList<String>();
				includedData.add("relationships");
				includedData.add("salesRanks");
				includedData.add("summaries");
				includedData.add("dimensions");
				if(skuRefresh.getRefreshTime()==null) {
					includedData.add("images");
				}	
				if(skuRefresh!=null&&skuRefresh.getAsin()!=null&&StrUtil.isNotBlank(skuRefresh.getAsin())) {
					CatalogApi api = apiBuildService.getCatalogApi(auth);
					api.getCatalogItemAsync(skuRefresh.getAsin(),market, includedData,null,callback);
				}else {
					String[] marketarray = skuRefresh.getMarketplaceid().split(",");
					for(String marketstr:marketarray) {
						// 先通过sku、marketplaceid、amazonauthid查询t_product_info获取pid
						List<ProductInfo> infoList = iProductInfoService.selectBySku(skuRefresh.getSku(), marketstr, auth.getId());
						if(infoList != null && infoList.size() > 0) {
							ProductInfo info = infoList.get(0);
							iAmzProductRefreshTypeService.updateRefreshTime(info.getId(), AmzProductRefreshType.TYPE_CATALOG, LocalDateTime.now());
						}
		    	}
				System.out.println("存在为空的ASIN："+skuRefresh.getSku());
			}
		}
	} catch (ApiException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		auth.setApiRateLimit( null, e);
	} catch (LWAException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		auth.setApiStatus(e);
	}
}
 
	@Override
	public void handlerResult(AmazonAuthority auth, AmzProductRefreshType skuRefresh,Item result) {
		log.info("handlerResult开始执行, authId={}, sku={}, asin={}, pid={}", 
				auth.getId(), skuRefresh.getSku(), skuRefresh.getAsin(), skuRefresh.getPid());
	   if(result==null) {
		   log.warn("handlerResult: result为null，直接返回, authId={}, sku={}", auth.getId(), skuRefresh.getSku());
		   return;
	   }
       ItemRelationships relist = result.getRelationships();
       log.info("handlerResult: relist={}, authId={}, sku={}", relist == null ? "null" : "size=" + relist.size(), auth.getId(), skuRefresh.getSku());
       // 如果pid未设置，通过sku查询产品信息获取pid
       if(skuRefresh.getPid()==null) {
    	   ProductInfo baseInfo = iProductInfoService.productOnlyone(auth.getId(), skuRefresh.getSku(), skuRefresh.getMarketplaceid());
    	   if(baseInfo!=null) {
    		   skuRefresh.setPid(new BigInteger(baseInfo.getId()));
    		   log.info("handlerResult: pid为空，通过sku查询补全pid={}", skuRefresh.getPid());
    	   } else {
    		   log.warn("handlerResult: pid为空且通过sku查询不到商品, authId={}, sku={}", auth.getId(), skuRefresh.getSku());
    	   }
       }
       // API调用成功，更新刷新时间（无论是否有relationships）
       if(skuRefresh.getPid()!=null) {
    	   boolean updated = iAmzProductRefreshTypeService.updateRefreshTime(
    			   skuRefresh.getPid().toString(), AmzProductRefreshType.TYPE_CATALOG, LocalDateTime.now());
    	   log.info("handlerResult: updateRefreshTime结果={}, pid={}, authId={}", updated, skuRefresh.getPid(), auth.getId());
       } else {
    	   log.warn("handlerResult: pid仍为null，无法更新refresh_time, authId={}, sku={}", auth.getId(), skuRefresh.getSku());
       }
       if(relist==null) {
    	   log.info("handlerResult: relist为null，跳过后续处理, authId={}, sku={}", auth.getId(), skuRefresh.getSku());
    	   return;
       }
       for(ItemRelationshipsByMarketplace relations:relist) {
    	  String marketplaceid= relations.getMarketplaceId();
    	    for(ItemRelationship rela:relations.getRelationships()) {
    	    	List<String> parentAsins = rela.getParentAsins();
    	    	List<String> children = rela.getChildAsins();
    	    	ProductInfo info = iProductInfoService.productOnlyone(auth.getId(),skuRefresh.getSku(),  marketplaceid);
    	    	if(info==null) {continue;}
    	    	if(parentAsins!=null&&parentAsins.size()>0) {
    	    		for(String parentAsin:parentAsins) {
        	    		if(StrUtil.isNotBlank(parentAsin)) {
            	    		if(info.getParentAsin()==null||!info.getParentAsin().equals(parentAsin)||Boolean.TRUE.equals(info.getIsparent())) {
            	    			info.setParentAsin(parentAsin);
            	    			info.setParentMarketplace(marketplaceid);
            	    			info.setIsparent(false);
            	    			iProductInfoService.updateById(info);
            	    			List<ProductInfo> asinParent = iProductInfoService.selectByAsin(auth.getId(), parentAsin ,marketplaceid);
            	    			if(asinParent!=null) {
            	    				for(ProductInfo parent:asinParent) {
										if(parent.getParentAsin()==null){
											parent.setIsparent(true);
											iProductInfoService.updateById(parent);
										}
                	    			}
            	    			}
            	    		}
            	    	}
        	    	}
    	    	}
    	    	else if(children!=null&&children.size()>0) {
    	    		boolean isparent = false;
    	    		for(String child:children) {
        	    		if(StrUtil.isNotBlank(child)) {
        	    			  List<ProductInfo> asinChildren = iProductInfoService.selectByAsin(auth.getId(), child ,marketplaceid);
            	    		  if(asinChildren!=null && asinChildren.size()>0) {
            	    			  for(ProductInfo childInfo:asinChildren) {
                	    			  if(childInfo.getParentAsin()==null||!childInfo.getParentAsin().equals(info.getAsin())) {
                	    				  childInfo.setParentAsin(info.getAsin());
                	    				  childInfo.setParentMarketplace(marketplaceid);
                	    				  childInfo.setIsparent(false);
                      	    			  iProductInfoService.updateById(childInfo);
                      	    		  }
                	    			  isparent=true;
                	    		  }
            	    		  }
            	    	}
        	    	}
    	    		if(isparent&&StrUtil.isBlank(info.getParentAsin())) {
    	    			if(info.getIsparent()==null||!info.getIsparent()) {
    	    				info.setIsparent(true);
    	    				iProductInfoService.updateById(info);
    	    			}
    	    		}
    	    	}
    	    	
    	    }
       }
       ItemSalesRanks salesRanks = result.getSalesRanks();
       if(salesRanks!=null) {
           for(ItemSalesRanksByMarketplace itemSalesRanksByMarketplace:salesRanks) {
        	   String marketplaceid= itemSalesRanksByMarketplace.getMarketplaceId();
    	       ProductInfo info = iProductInfoService.productOnlyone(auth.getId(),skuRefresh.getSku(),  marketplaceid);
    	       if(info==null) {continue;}
    	       List<ItemClassificationSalesRank> classificationRanks = itemSalesRanksByMarketplace.getClassificationRanks();
    	       List<ItemDisplayGroupSalesRank> displayGroupRanks = itemSalesRanksByMarketplace.getDisplayGroupRanks();
    	       for(ItemClassificationSalesRank itemClassificationSalesRank:classificationRanks) {
    	    	   Integer rank = itemClassificationSalesRank.getRank();
    	    	   //String link=itemClassificationSalesRank.getLink();
    	    	   String categoryId=itemClassificationSalesRank.getClassificationId();
    	    	   //String title=itemClassificationSalesRank.getTitle();
    	    	   ProductRank productRank=new ProductRank();
    	    	   productRank.setByday(new Date());
    	    	   productRank.setCategoryid(categoryId);
    	    	   productRank.setIsmain(false);
    	    	   productRank.setRank(rank);
    	    	   productRank.setTitle(itemClassificationSalesRank.getTitle());
    	    	   productRank.setLink(itemClassificationSalesRank.getLink());
    	    	   productRank.setProductId(info.getId());
    	    	   iProductRankService.insert(productRank);
    	       }
    	       for(ItemDisplayGroupSalesRank itemDisplayGroupSalesRank:displayGroupRanks) {
    	    	   //String  link=itemDisplayGroupSalesRank.getLink();
    	    	   Integer rank = itemDisplayGroupSalesRank.getRank();
    	    	   //String title=itemDisplayGroupSalesRank.getTitle();
    	    	   String categoryId=itemDisplayGroupSalesRank.getWebsiteDisplayGroup();
    	    	   ProductRank productRank=new ProductRank();
    	    	   productRank.setByday(new Date());
    	    	   productRank.setCategoryid(categoryId);
    	    	   productRank.setIsmain(true);
    	    	   productRank.setRank(rank);
    	    	   productRank.setTitle(itemDisplayGroupSalesRank.getTitle());
    	    	   productRank.setLink(itemDisplayGroupSalesRank.getLink());
    	    	   productRank.setProductId(info.getId());
    	    	   iProductRankService.insert(productRank);
    	       }
           }
       }
		ItemDimensions dimensions= result.getDimensions();
		if(dimensions!=null && !dimensions.isEmpty()){
			for(ItemDimensionsByMarketplace dim:dimensions){
				ProductInfo info = iProductInfoService.productOnlyone(auth.getId(),skuRefresh.getSku(),  dim.getMarketplaceId());
				if(info==null) {continue;}
				if(dim.getItem()!= null){
					DimensionsInfo item=new DimensionsInfo();
					if(  dim.getItem().getLength()!=null){
						item.setLength(dim.getItem().getLength().getValue());
						item.setLengthUnits(dim.getItem().getLength().getUnit());
					}
					if(dim.getItem().getWidth()!=null){
						item.setWidth(dim.getItem().getWidth().getValue());
						item.setWidthUnits(dim.getItem().getWidth().getUnit());
					}
					if(dim.getItem().getHeight()!=null){
						item.setHeight(dim.getItem().getHeight().getValue());
						item.setHeightUnits(dim.getItem().getHeight().getUnit());
					}
					if(dim.getItem().getWeight()!=null){
						item.setWeight(dim.getItem().getWeight().getValue());
						item.setWeightUnits(dim.getItem().getWeight().getUnit());
					}
					dimensionsInfoMapper.insert(item);
					info.setItemDimensions(new BigInteger(item.getId()));
				}
				if(dim.getPackage()!=null && dim.getPackage().getLength()!=null){
					DimensionsInfo item=new DimensionsInfo();
					if(dim.getPackage().getLength()!=null){
						item.setLength(dim.getPackage().getLength().getValue());
						item.setLengthUnits(dim.getPackage().getLength().getUnit());
					}
					if(dim.getPackage().getWidth()!=null){
						item.setWidth(dim.getPackage().getWidth().getValue());
						item.setWidthUnits(dim.getPackage().getWidth().getUnit());
					}
					if(dim.getPackage().getHeight()!=null){
						item.setHeight(dim.getPackage().getHeight().getValue());
						item.setHeightUnits(dim.getPackage().getHeight().getUnit());
					}
					if(dim.getPackage().getWeight()!=null){
						item.setWeight(dim.getPackage().getWeight().getValue());
						item.setWeightUnits(dim.getPackage().getWeight().getUnit());
					}
					dimensionsInfoMapper.insert(item);
					info.setPageDimensions(new BigInteger(item.getId()));
				}
				iProductInfoService.updateById(info);
			}


		}
       ItemSummaries summary = result.getSummaries();
       String imageurl = null;
	if(summary!=null&&summary.size()>0) {
    	   for(ItemSummaryByMarketplace item:summary) {
    		   String brand=null;
    		   String manufacturer=null;
    		   if(item.getBrand()!=null) {
    			   brand=item.getBrand();
    		   }
    		   if(item.getManufacturer()!=null) {
    			   manufacturer=item.getManufacturer();
    		   }
			   ProductInfo info = iProductInfoService.productOnlyone(auth.getId(),skuRefresh.getSku(),  item.getMarketplaceId());
			   if(info==null) {
				   continue;
			   }
			   if(info.getName()==null) {
				   info.setName(item.getItemName());
			   }
		       info.setBrand(brand);
		       if(result.getProductTypes()!=null&&result.getProductTypes().size()>0) {
		    	   info.setTypename(result.getProductTypes().get(0).getProductType());
		       }
		       info.setManufacturer(manufacturer);
		       if(result.getImages()!=null&&result.getImages().size()>0) {
					ItemImagesByMarketplace image = result.getImages().get(0);
					List<ItemImage> images = image.getImages();
					if(images!=null&&images.size()>0) {
						for(ItemImage itemimage:images) {
							if(itemimage.getVariant().compareTo(VariantEnum.MAIN)==0&&itemimage.getWidth()==75) {
								imageurl=itemimage.getLink();
								break;
							}
						}
					}
					Picture picture=null;
				    try {
				    	if(imageurl!=null) {
				    		if(info.getImage()!=null) {
					    		picture=pictureService.getById(info.getImage());
					    		if(picture==null
					    				||picture.getUrl()==null
					    				||picture.getLocation()==null
					    				||!picture.getUrl().equals(imageurl)
					    				) {
					    			if(picture!=null&&picture.getUrl()!=null&&!picture.getUrl().equals(imageurl)){
					    				String path=PictureServiceImpl.productImgPath+auth.getShopId()+"/"+auth.getId()+"/"+info.getMarketplaceid()+"/";
						    			picture=pictureService.downloadPicture(imageurl,path, info.getImage().toString());
						    			manufacturer =manufacturer!=null?manufacturer:"";
						    			info.setManufacturer(manufacturer+"-c");
					    			}
					    			if(picture!=null&&picture.getOpttime()!=null&&GeneralUtil.distanceOfDay(picture.getOpttime(), new Date())<3) {
					    				 //do nothing
					    			}else {
					    				String path=PictureServiceImpl.productImgPath+auth.getShopId()+"/"+auth.getId()+"/"+info.getMarketplaceid()+"/";
						    			picture=pictureService.downloadPicture(imageurl,path, info.getImage().toString());
						    			manufacturer =manufacturer!=null?manufacturer:"";
						    			info.setManufacturer(manufacturer+"-c");
					    			}
					    			
					    			
					    		}
					    	}else {
					    		String path=PictureServiceImpl.productImgPath+auth.getShopId()+"/"+auth.getId()+"/"+info.getMarketplaceid()+"/";
					    		picture=pictureService.downloadPicture(imageurl, path,null);
					    		manufacturer =manufacturer!=null?manufacturer:"";
				    			info.setManufacturer(manufacturer+"-c");
					    	}
				    	}
				    	 if(picture!=null) {
			    		       	info.setImage(new BigInteger(picture.getId()));
			    		  }
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
		       iProductInfoService.updateById(info);
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
					iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_CATALOG, LocalDateTime.now());
				}else {
					iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_CATALOG, LocalDateTime.now());
					ProductInfo info = iProductInfoService.getById(pid);
					info.setInvalid(true);
					iProductInfoService.updateById(info);
				}
			}else {
				iAmzProductRefreshTypeService.updateRefreshTime(pid, AmzProductRefreshType.TYPE_CATALOG, LocalDateTime.now());
			}
		}
	}

    boolean isrun=true;
	@Override
	public void stopTask() {
		// TODO Auto-generated method stub
		isrun=false;
	}


	@Override
	public void runTask() {
		// TODO Auto-generated method stub
		ProductCatalogItemServiceImpl self = this;
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(isrun) {
					try {
						amazonAuthorityService.executTask(self);
						Thread.sleep(5000);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}).start();
	}
 
	
	@Override
	public List<String> captureCatalogProductChildren(AmazonAuthority auth, String asin, List<String> market) {
		// TODO Auto-generated method stub
		List<String> includedData=new ArrayList<String>();
		includedData.add("relationships");
		Item res = this.captureCatalogProduct(auth, asin, market,includedData);
		if(res!=null&&res.getRelationships()!=null) {
			ItemRelationshipsByMarketplace rela = res.getRelationships().get(0);
			if(rela!=null&&rela.getRelationships().size()>0) {
				 List<String> parent = rela.getRelationships().get(0).getParentAsins();
				 if(parent!=null&&parent.size()>0) {
					   res = this.captureCatalogProduct(auth, parent.get(0), market,includedData);
				 }
			}
		}
		List<String> asins=new ArrayList<String>();
		for(ItemRelationshipsByMarketplace mkrelation: res.getRelationships()) {
		  for(ItemRelationship rela:mkrelation.getRelationships()) {
  	    	List<String> children = rela.getChildAsins();
  	    	for(String childasin:children) {
  	    		asins.add(childasin);
  	    	}
		  }
		}
		if(asins.size()==0) {
			asins.add(asin);
		}
		return asins;
	}
	
	public List<Item> captureCatalogProduct( AmazonAuthority auth, List<String> asins, List<String> market ){
		List<String> includedDataAttr=new ArrayList<String>();
    	includedDataAttr.add("summaries");
    	includedDataAttr.add("images");
		List<Item> result=new ArrayList<Item>();
		 if(asins.size()>0) {
 			try {
 				List<List<String>> listasin = GeneralUtil.getListByPageSize(asins, 20);
 				for(List<String> itemasinlist:listasin) {
 					ProductCatalogItemsDTO dto=new ProductCatalogItemsDTO();
	    				dto.setIdentifiersType("ASIN");
	    				dto.setIdentifiers(itemasinlist);
	    				dto.setIncludedData(includedDataAttr);
	    				dto.setMarketplaceIds(market);
	    				dto.setSellerId(auth.getSellerid());
	    				dto.setPageSize(20);
						ItemSearchResults itemres = this.searchCatalogProducts(auth, dto);
						for(Item res2:itemres.getItems()) {
							if(res2!=null&&res2.getSummaries()!=null) {
		  	    				for(ItemSummaryByMarketplace sitem:res2.getSummaries()) {
		  	    					String marketplaceid=sitem.getMarketplaceId();
		  	    					Marketplace marketobj = iMarketplaceService.findMapByMarketplaceId().get(marketplaceid);
		  	    					sitem.setWebsiteDisplayGroup("https://www."+marketobj.getPointName()+"/dp/"+res2.getAsin());
		  	    				}
		  	    			}
		  	    			result.add(res2);
	    				 }
 				}
 			}catch(Exception e) {
 				e.printStackTrace();
 			}
 	 
 	    } 
		return result;
	}
}
