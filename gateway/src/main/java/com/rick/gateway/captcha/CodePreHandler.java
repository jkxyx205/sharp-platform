package com.rick.gateway.captcha;

/**
 * 验证码发送前的业务预检查：在 captcha.types.{type}.pre-handler 中按 Spring Bean 名配置，
 * 由 {@link ValidateCodeService#sendCode} 在生成/存储/发送之前调用（仅 mobile 有值时）。
 * <p>
 * 阻塞实现须保证调用方处于 boundedElastic 线程（现有短信发送链路已满足）。
 */
public interface CodePreHandler {

    /**
     * @param mobile 收码手机号
     * @return true 继续发送；false 中断发送（400 + 默认文案）；
     *         需要向前端返回具体原因时直接抛 {@link PreHandlerException}
     */
    boolean handler(String mobile);
}
