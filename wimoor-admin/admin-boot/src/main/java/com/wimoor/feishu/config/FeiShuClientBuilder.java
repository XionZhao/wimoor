package com.wimoor.feishu.config;


import com.lark.oapi.Client;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.feishu.pojo.entity.Auth;
import com.wimoor.feishu.service.IAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Slf4j
@Configuration
public class FeiShuClientBuilder {
     @Autowired
     IAuthService iAuthService;
	public Client getClient(String appid) {
		Auth auth = null;
		UserInfo userinfo = UserInfoContext.get();
		if (userinfo != null && userinfo.getCompanyid() != null) {
			auth = iAuthService.lambdaQuery().eq(Auth::getAppId, appid).eq(Auth::getShopid, userinfo.getCompanyid()).one();
		}
		List<Auth> list = null;
		// 无用户上下文时（如飞书webhook回调），通过appid直接查询
		if (auth == null) {
			list = iAuthService.lambdaQuery().eq(Auth::getAppId, appid).list();
			if (list != null && !list.isEmpty()) {
				auth = list.get(0);
			}
		}
		if (auth == null) {
			// 兼容不带cli_前缀的情况
			list = iAuthService.lambdaQuery().eq(Auth::getAppId, "cli_" + appid).list();
			if (list != null && !list.isEmpty()) {
				auth = list.get(0);
			}
		}
		if (auth == null) {
			throw new RuntimeException("未找到飞书应用配置: appid=" + appid);
		}
		return Client.newBuilder(auth.getAppId(), auth.getAppSecret()).logReqAtDebug(true).build();
	}

}