package com.wimoor.feishu.service.impl;

import com.wimoor.feishu.pojo.entity.Auth;
import com.wimoor.feishu.mapper.AuthMapper;
import com.wimoor.feishu.service.IAuthService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wimoor team
 * @since 2023-09-01
 */
@Service
public class AuthServiceImpl extends ServiceImpl<AuthMapper, Auth> implements IAuthService {

    @Override
    public String getShopIdByAppId(String appId) {
        // 查询该appId对应的所有记录，按opttime升序排列，取最早的那条
        List<Auth> authList = lambdaQuery()
                .eq(Auth::getAppId, appId)
                .orderByAsc(Auth::getOpttime)
                .last("LIMIT 1")
                .list();
        
        if (authList != null && !authList.isEmpty()) {
            return authList.get(0).getShopid();
        }
        return null;
    }
}
