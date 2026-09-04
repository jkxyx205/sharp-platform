package com.rick.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 支持 lb:// 服务名的 WebClient.Builder：
 * spring-cloud-commons 的 LoadBalancerWebClientBuilderBeanPostProcessor
 * 会自动为 @LoadBalanced 标记的 builder 装配服务发现。
 */
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
