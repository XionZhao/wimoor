package com.wimoor.erp.thirdparty.service.impl;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.wimoor.common.GeneralUtil;
import com.wimoor.common.HttpClientUtil;
import com.wimoor.common.mvc.BizException;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.ship.pojo.entity.ShipTransCompany;
import com.wimoor.erp.thirdparty.pojo.entity.ThirdPartyAPI;
import com.wimoor.erp.thirdparty.service.IShipTransCompanyWTOService;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

@Slf4j
@Service("shipTransCompanyWTOService")
public class ShipTransCompanyWTOServiceImpl implements IShipTransCompanyWTOService {

	@Override
	public JSONObject getApiJson(UserInfo user, ThirdPartyAPI companyapi, ShipTransCompany stcompany, String shipmentid) {
		try {
			String tokenId = companyapi.getAppkey();
			String accessToken = companyapi.getAppsecret();
			
			if (StrUtil.isBlank(tokenId) || StrUtil.isBlank(accessToken)) {
				throw new BizException("TokenID或AccessToken未配置，请在API配置中设置appkey和appsecret");
			}
			
			String baseUrl = companyapi.getApi();
			
			// 1. 先查询派送单号
			String trackNumberUrl = baseUrl + "/api/ApiService/QueryTrackNumber?hawbCode=" + shipmentid;
			JSONObject trackNumberResult = doGetRequest(trackNumberUrl, tokenId, accessToken);
			
			JSONObject result = new JSONObject();
			result.put("ftype", "WTO");
			result.put("trackNumber", trackNumberResult);
			
			// 2. 再查询轨迹信息
			String trackUrl = baseUrl + "/api/ApiService/QueryTrack?hawbCode=" + shipmentid;
			JSONObject trackResult = doGetRequest(trackUrl, tokenId, accessToken);
			result.put("track", trackResult);
			
			return result;
		} catch (Exception e) {
			log.error("调用WTO API失败", e);
			throw new BizException("调用WTO API失败: " + e.getMessage());
		}
	}
	
	private JSONObject doGetRequest(String url, String tokenId, String accessToken) {
		try {
			// 获取当前时间戳（秒）
			long timestamp = System.currentTimeMillis() / 1000;
			String timestampStr = String.valueOf(timestamp);
			
			// 计算签名: MD5(TokenID + AccessToken + Timestamp) 32位大写
			String signStr = tokenId + accessToken + timestampStr;
			String sign = md5(signStr).toUpperCase();
			
			// 设置请求头
			Map<String, String> headers = new HashMap<>();
			headers.put("TokenID", tokenId);
			headers.put("Timestamp", timestampStr);
			headers.put("Sign", sign);
			headers.put("Content-Type", "application/json");
			
			String response = HttpClientUtil.getUrl(url, headers);
			
			if (StrUtil.isBlank(response)) {
				return null;
			}
			
			return GeneralUtil.getJsonObject(response);
		} catch (Exception e) {
			log.error("请求WTO API失败: " + url, e);
			return null;
		}
	}
	
	private String md5(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger number = new BigInteger(1, messageDigest);
			return number.toString(16);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
