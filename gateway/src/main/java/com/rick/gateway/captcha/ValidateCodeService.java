package com.rick.gateway.captcha;

import com.rick.sms.core.ValidateCodeSender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;

/**
 * 验证码服务：统一发送入口，按 type 配置分发——
 * 图片：生成并存储（由控制器渲染图片返回）；短信：按 type 选择模板发送。
 * <p>
 * {@code sendCode} 为响应式：先频率限制（前置，超限即拒以省掉后续 preCheck HTTP），
 * 再业务预检查（阻塞 HTTP，调度到 boundedElastic），生成码、存储、短信发送（阻塞，调度到 boundedElastic）。
 * {@code verify}/{@code consume} 亦返回 Mono，下游 {@link ValidateCodeFilter} 在响应式链内非阻塞调用。
 */
@Service
public class ValidateCodeService implements InitializingBean {

    private final ValidateCodeProperties properties;
    private final ValidateCodeStore store;
    private final ValidateCodeSender validateCodeSender;
    private final CodeRateLimiter rateLimiter;
    /** beanName -> 预检查实现（Spring 按类型收集所有 CodePreHandler Bean） */
    private final Map<String, CodePreHandler> preHandlers;
    private final SecureRandom random = new SecureRandom();

    public ValidateCodeService(ValidateCodeProperties properties,
                               ValidateCodeStore store,
                               ValidateCodeSender validateCodeSender,
                               CodeRateLimiter rateLimiter,
                               Map<String, CodePreHandler> preHandlers) {
        this.properties = properties;
        this.store = store;
        this.validateCodeSender = validateCodeSender;
        this.rateLimiter = rateLimiter;
        this.preHandlers = preHandlers;
    }

    /** 启动期校验配置的 preHandler bean 名存在，yml 笔误直接启动失败 */
    @Override
    public void afterPropertiesSet() {
        properties.getTypes().forEach((type, spec) -> {
            if (StringUtils.hasText(spec.getPreHandler())) {
                Assert.isTrue(preHandlers.containsKey(spec.getPreHandler()),
                        "captcha.types." + type + ".pre-handler 未找到 CodePreHandler Bean: " + spec.getPreHandler());
            }
        });
    }

    /**
     * 统一发送入口：频率限制 → 业务预检查 → 生成验证码并存储；短信类型同时选择模板发送，图片类型由调用方渲染返回。
     *
     * @param ip 客户端 IP，用于频率限制的 IP 维度（图片码 mobile 为 null）
     * @return 生成的验证码
     */
    public Mono<ValidateCode> sendCode(String type, String mobile, String deviceId, String ip) {
        ValidateCodeProperties.TypeSpec spec = typeSpec(type);
        return rateLimiter.check(spec, mobile, deviceId, ip)
                // 预检查可能阻塞 HTTP（如 registerCodePreHandler 查 platform），调度到 boundedElastic
                .then(Mono.fromCallable(() -> {
                    preCheck(spec, mobile);
                    return spec;
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(s -> {
                    ValidateCode code = new ValidateCode(
                            s.getKind() == CodeKind.IMAGE ? imageCode() : smsCode(),
                            expireAt(s));
                    String key = ValidateCodeStore.buildKey(s.getKind(), mobile, deviceId, type);
                    return store.put(key, code)
                            .then(Mono.defer(() -> {
                                if (s.getKind() != CodeKind.SMS) {
                                    return Mono.just(code);
                                }
                                return Mono.fromCallable(() -> {
                                            validateCodeSender.send(mobile, properties.getSignName(), s.getTemplate(), code.content());
                                            return code;
                                        }).subscribeOn(Schedulers.boundedElastic())
                                        // 发送失败回滚已存码，避免「显示已发送、永远校验不过」
                                        .onErrorResume(e -> store.remove(key).then(Mono.error(e)));
                            }))
                            .thenReturn(code);
                });
    }

    /**
     * 业务预检查：type 配置了 pre-handler 且 mobile 有值才执行（图片码无 mobile，天然跳过）。
     * handler 返回 false → 默认文案拒绝；抛异常 → 原样冒泡（携带业务文案）。
     */
    private void preCheck(ValidateCodeProperties.TypeSpec spec, String mobile) {
        if (!StringUtils.hasText(spec.getPreHandler()) || !StringUtils.hasText(mobile)) {
            return;
        }
        CodePreHandler handler = preHandlers.get(spec.getPreHandler());
        if (!handler.handler(mobile)) {
            throw new PreHandlerException("当前业务不允许发送验证码");
        }
    }

    public enum VerifyResult {
        OK, NOT_FOUND_OR_EXPIRED, MISMATCH
    }

    /**
     * 校验（不消费）：比对成功返回 OK，验证码保留；比对失败不删除（有效期内可重试）。
     * 业务处理成功后由调用方调用 {@link #consume} 消费，业务失败则验证码仍可用于重试。
     */
    public Mono<VerifyResult> verify(CodeKind kind, String type, String mobile, String deviceId, String input) {
        String key = ValidateCodeStore.buildKey(kind, mobile, deviceId, type);
        return store.get(key)
                .map(stored -> stored.matches(input) ? VerifyResult.OK : VerifyResult.MISMATCH)
                .defaultIfEmpty(VerifyResult.NOT_FOUND_OR_EXPIRED);
    }

    /**
     * 消费验证码：仅当存储中仍是同一验证码时原子删除（期间重新发码不会误删新码）。
     */
    public Mono<Void> consume(CodeKind kind, String type, String mobile, String deviceId) {
        String key = ValidateCodeStore.buildKey(kind, mobile, deviceId, type);
        return store.get(key).flatMap(stored -> store.consume(key, stored)).then();
    }

    public ValidateCodeProperties.TypeSpec typeSpec(String type) {
        ValidateCodeProperties.TypeSpec spec = properties.getTypes().get(type);
        if (spec == null) {
            throw new IllegalArgumentException("验证码类型未配置: " + type);
        }
        return spec;
    }

    private String smsCode() {
        int length = properties.getSms().getLength();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String imageCode() {
        String chars = properties.getImage().getChars();
        int length = properties.getImage().getLength();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private Instant expireAt(ValidateCodeProperties.TypeSpec spec) {
        long seconds = spec.getExpireSeconds() != null ? spec.getExpireSeconds() : properties.getExpireSeconds();
        return Instant.now().plusSeconds(seconds);
    }
}
