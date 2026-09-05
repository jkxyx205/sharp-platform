package com.rick.common.component.starter.dubbo;

import com.rick.common.component.starter.config.ComponentConfig;
import com.rick.common.component.starter.model.User;
import com.rick.common.component.starter.model.UserContextHolder;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

/**
 * Dubbo 消费端：把当前线程的用户上下文以 attachment 透传给提供方。
 * 提供方线程不经过 Spring MVC，靠 attachment 重建上下文（见 UserContextProviderFilter）。
 * 链式调用天然支持：本过滤器读的是 ThreadLocal。
 */
@Activate(group = CommonConstants.CONSUMER)
public class UserContextConsumerFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        User user = UserContextHolder.get();
        if (user != null) {
            if (user.getId() != null) {
                invocation.setAttachment(ComponentConfig.HEADER_USER_ID, String.valueOf(user.getId()));
            }
            if (user.getMobile() != null) {
                invocation.setAttachment(ComponentConfig.HEADER_USER_MOBILE, user.getMobile());
            }
            if (user.getGroupId() != null) {
                invocation.setAttachment(ComponentConfig.HEADER_GROUP_ID, String.valueOf(user.getGroupId()));
            }
        }
        return invoker.invoke(invocation);
    }
}
