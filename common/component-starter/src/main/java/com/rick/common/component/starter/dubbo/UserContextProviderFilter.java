package com.rick.common.component.starter.dubbo;

import com.rick.common.component.starter.config.ComponentConfig;
import com.rick.common.component.starter.model.User;
import com.rick.common.component.starter.model.UserContextHolder;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Dubbo 提供端：从 attachment 还原消费端用户上下文放入 UserContextHolder，
 * 使 @DubboService 内的数据库访问（DatabaseConfig 拼 group_id）与 HTTP 链路一致。
 * 结束后恢复原上下文：远程调用原值为 null（等价 remove），injvm 嵌套调用时不破坏外层。
 */
@Activate(group = CommonConstants.PROVIDER)
public class UserContextProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String userId = invocation.getAttachment(ComponentConfig.HEADER_USER_ID);
        if (userId == null || userId.isBlank()) {
            return invoker.invoke(invocation);
        }
        User user = new User();
        user.setId(Long.parseLong(userId));
        user.setMobile(invocation.getAttachment(ComponentConfig.HEADER_USER_MOBILE));
        String groupId = invocation.getAttachment(ComponentConfig.HEADER_GROUP_ID);
        if (groupId != null && !groupId.isBlank()) {
            user.setGroupId(Long.parseLong(groupId));
        }
        User previous = UserContextHolder.get();
        UserContextHolder.put(user);
        try {
            return invoker.invoke(invocation);
        } finally {
            if (previous != null) {
                UserContextHolder.put(previous);
            } else {
                UserContextHolder.remove();
            }
        }
    }
}
