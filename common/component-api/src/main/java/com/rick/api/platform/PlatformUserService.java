package com.rick.api.platform;


import java.util.Collection;
import java.util.Map;

public interface PlatformUserService {

    Map<Long, String> getIdNicknameMap(Collection<Long> ids);
}
