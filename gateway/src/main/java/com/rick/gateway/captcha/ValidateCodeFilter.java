package com.rick.gateway.captcha;

import com.rick.gateway.security.TokenStore;
import com.rick.gateway.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 验证码校验过滤器：匹配配置的业务 URL 规则，取 deviceId（请求头）、mobile（自定义方法 → 用户上下文 →
 * query 参数兜底）与客户端提交的 code（query 参数 code，兼容请求头 code），比对存储的验证码，通过后一次性消费。
 * <p>
 * 注意：不能注册为 Spring Bean —— WebFlux 会把所有 WebFilter Bean 自动挂进全局过滤链，
 * 导致其在 security 链之外重复执行。由 SecurityConfig 实例化并加入 security 链 AUTHORIZATION 之后：
 * 未认证访问受保护接口先得到 401，验证码错误才是 400。
 */
public class ValidateCodeFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(ValidateCodeFilter.class);

    public static final String HEADER_DEVICE_ID = "deviceId";
    public static final String PARAM_CODE = "code";
    public static final String PARAM_MOBILE = "mobile";

    private record CompiledRule(PathPattern pattern, String type, ValidateCodeProperties.TypeSpec spec,
                                MobileResolver mobileResolver) {
    }

    private final List<CompiledRule> rules;
    private final ValidateCodeService service;
    private final TokenStore tokenStore;

    public ValidateCodeFilter(ValidateCodeProperties properties, ValidateCodeService service,
                              TokenStore tokenStore, ApplicationContext context) {
        this.service = service;
        this.tokenStore = tokenStore;
        this.rules = compile(properties, context);
    }

    /** 启动期预编译规则，url / type / mobile 方法引用非法直接启动失败 */
    private static List<CompiledRule> compile(ValidateCodeProperties properties, ApplicationContext context) {
        List<CompiledRule> compiled = new ArrayList<>(properties.getRules().size());
        for (ValidateCodeProperties.Rule rule : properties.getRules()) {
            PathPattern pattern;
            try {
                pattern = PathPatternParser.defaultInstance.parse(rule.getUrl());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("captcha.rules[].url 非法: " + rule.getUrl(), e);
            }
            String type = rule.effectiveType();
            ValidateCodeProperties.TypeSpec spec = properties.getTypes().get(type);
            Assert.notNull(spec, "captcha.types 缺少 type 配置: " + type + "（url: " + rule.getUrl() + "）");
            MobileResolver resolver = StringUtils.hasText(rule.getMobile())
                    ? MobileResolver.compile(rule.getMobile(), context)
                    : null;
            compiled.add(new CompiledRule(pattern, type, spec, resolver));
        }
        return List.copyOf(compiled);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        CompiledRule rule = match(exchange.getRequest());
        if (rule == null) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest();
        String deviceId = request.getHeaders().getFirst(HEADER_DEVICE_ID);
        if (!StringUtils.hasText(deviceId)) {
            return reject(exchange, "缺少设备标识 deviceId");
        }
        String code = request.getQueryParams().getFirst(PARAM_CODE);
        if (!StringUtils.hasText(code)) {
            code = request.getHeaders().getFirst(PARAM_CODE);
        }
        if (!StringUtils.hasText(code)) {
            return reject(exchange, "验证码不能为空");
        }
        final String finalCode = code;
        return currentUser()
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(user -> verify(exchange, chain, rule, deviceId, finalCode, user.orElse(null)));
    }

    private Mono<Void> verify(ServerWebExchange exchange, WebFilterChain chain, CompiledRule rule,
                              String deviceId, String code, User user) {
        String mobile = null;
        if (rule.spec().getKind() == CodeKind.SMS) {
            try {
                mobile = resolveMobile(rule, user, exchange.getRequest());
            } catch (RuntimeException e) {
                log.warn("自定义 mobile 解析方法执行失败, type={}: {}", rule.type(), e.toString());
                return reject(exchange, "获取手机号失败");
            }
            if (!StringUtils.hasText(mobile)) {
                return reject(exchange, "无法获取手机号");
            }
        }
        ValidateCodeService.VerifyResult result =
                service.verify(rule.spec().getKind(), rule.type(), mobile, deviceId, code);
        return switch (result) {
            case OK -> chain.filter(exchange);
            case NOT_FOUND_OR_EXPIRED -> reject(exchange, "验证码不存在或已失效");
            case MISMATCH -> reject(exchange, "验证码错误");
        };
    }

    /**
     * sms 规则 mobile 解析顺序：自定义方法 → 用户上下文 → query 参数 mobile（匿名接口兜底）。
     */
    private String resolveMobile(CompiledRule rule, User user, ServerHttpRequest request) {
        if (rule.mobileResolver() != null) {
            String mobile = rule.mobileResolver().resolve(user, rule.type());
            if (StringUtils.hasText(mobile)) {
                return mobile;
            }
        }
        if (user != null && StringUtils.hasText(user.mobile())) {
            return user.mobile();
        }
        return request.getQueryParams().getFirst(PARAM_MOBILE);
    }

    /**
     * token 用户上下文（Authentication.credentials 即 token），未认证时为空。
     */
    private Mono<User> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(context -> context.getAuthentication() == null
                        ? null : context.getAuthentication().getCredentials())
                .ofType(String.class)
                .flatMap(token -> Mono.justOrEmpty(tokenStore.findUserInfo(token)))
                .map(info -> new User(info.userId(), info.mobile()));
    }

    /** 规则 url 匹配的是网关原始路径（StripPrefix 之前） */
    private CompiledRule match(ServerHttpRequest request) {
        for (CompiledRule rule : rules) {
            if (rule.pattern().matches(request.getPath().pathWithinApplication())) {
                return rule;
            }
        }
        return null;
    }

    private static Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory()
                .wrap(CaptchaJson.error(400, message).getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
