package com.rick.gateway.captcha;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 注册验证码预检查：手机号已注册则拒绝发码（400「该手机号已注册」），
 * 避免无效注册流程与短信费用。注册状态经 lb://platform HTTP 内部调用查询
 * （网关统一 HTTP，Dubbo 仅用于后端服务间调用）。
 * <p>
 * {@code block()} 阻塞调用发生在 boundedElastic 线程（短信发送链路既有约定）；
 * platform 不可用时异常冒泡，落入 SmsController 的 500 兜底。
 */
@Component("registerCodePreHandler")
public class RegisterCodePreHandler implements CodePreHandler {

    private final WebClient webClient;

    // @LoadBalanced 同时作为限定符，匹配 WebClientConfig 中带此注解的 builder
    public RegisterCodePreHandler(@LoadBalanced WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public boolean handler(String mobile) {
        Boolean exists = webClient.get()
                .uri("lb://platform/auth/mobile_exists?mobile={mobile}", mobile)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
        if (Boolean.TRUE.equals(exists)) {
            throw new PreHandlerException("该手机号已注册");
        }
        return true;
    }
}
