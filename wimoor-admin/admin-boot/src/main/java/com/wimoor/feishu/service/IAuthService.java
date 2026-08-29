package com.wimoor.feishu.service;

import com.wimoor.feishu.pojo.entity.Auth;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wimoor team
 * @since 2023-09-01
 */
public interface IAuthService extends IService<Auth> {

    /**
     * 通过appId获取shopid（使用绑定时间最早的那条记录）
     * @param appId 飞书应用ID
     * @return shopid
     */
    String getShopIdByAppId(String appId);
}
