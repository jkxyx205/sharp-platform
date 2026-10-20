package com.rick.platform.module.user.service.dubbo;

import com.rick.api.platform.PlatformUserService;
import com.rick.platform.module.user.service.UserService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Collection;
import java.util.Map;

@DubboService
public class PlatformUserServiceImpl implements PlatformUserService {

    @Resource
    UserService userService;

    @Override
    public Map<Long, String> getIdNicknameMap(Collection<Long> ids) {
        return userService.getIdNicknameMap(ids);
    }
}
