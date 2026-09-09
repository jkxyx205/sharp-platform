package com.rick.gateway.captcha;

import com.rick.sms.core.ValidateCodeSender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;

/**
 * 验证码服务：统一发送入口，按 type 配置分发——
 * 图片：生成并存储（由控制器渲染图片返回）；短信：按 type 选择模板发送。
 * <p>
 * 短信发送为阻塞调用，调用方须置于 {@code Schedulers.boundedElastic()} 线程，勿在事件循环线程调用。
 */
@Service
public class ValidateCodeService implements InitializingBean {

    private final ValidateCodeProperties properties;
    private final ValidateCodeStore store;
    private final ValidateCodeSender validateCodeSender;
    /** beanName -> 预检查实现（Spring 按类型收集所有 CodePreHandler Bean） */
    private final Map<String, CodePreHandler> preHandlers;
    private final SecureRandom random = new SecureRandom();

    public ValidateCodeService(ValidateCodeProperties properties,
                               ValidateCodeStore store,
                               ValidateCodeSender validateCodeSender,
                               Map<String, CodePreHandler> preHandlers) {
        this.properties = properties;
        this.store = store;
        this.validateCodeSender = validateCodeSender;
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
     * 统一发送入口：先执行 type 配置的业务预检查，再按 type 生成验证码并存储；
     * 短信类型同时选择模板发送，图片类型由调用方渲染返回的内容。
     *
     * @return 生成的验证码
     */
    public ValidateCode sendCode(String type, String mobile, String deviceId) {
        ValidateCodeProperties.TypeSpec spec = typeSpec(type);
        preCheck(spec, mobile);
        String content = spec.getKind() == CodeKind.IMAGE ? imageCode() : smsCode();
        ValidateCode code = new ValidateCode(content, expireAt(spec));
        String key = ValidateCodeStore.buildKey(spec.getKind(), mobile, deviceId, type);
        store.put(key, code);
        if (spec.getKind() == CodeKind.SMS) {
            try {
                validateCodeSender.send(mobile, properties.getSignName(), spec.getTemplate(), content);
            } catch (RuntimeException e) {
                // 发送失败回滚已存码，避免"显示已发送、永远校验不过"
                store.remove(key);
                throw e;
            }
        }
        return code;
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
    public VerifyResult verify(CodeKind kind, String type, String mobile, String deviceId, String input) {
        String key = ValidateCodeStore.buildKey(kind, mobile, deviceId, type);
        ValidateCode stored = store.get(key).orElse(null);
        if (stored == null) {
            return VerifyResult.NOT_FOUND_OR_EXPIRED;
        }
        return stored.matches(input) ? VerifyResult.OK : VerifyResult.MISMATCH;
    }

    /**
     * 消费验证码：仅当存储中仍是同一验证码时原子删除（期间重新发码不会误删新码）。
     */
    public void consume(CodeKind kind, String type, String mobile, String deviceId) {
        String key = ValidateCodeStore.buildKey(kind, mobile, deviceId, type);
        store.get(key).ifPresent(code -> store.consume(key, code));
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
