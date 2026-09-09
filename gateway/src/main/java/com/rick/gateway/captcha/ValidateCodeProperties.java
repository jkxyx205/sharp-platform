package com.rick.gateway.captcha;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 验证码配置：有效期、内容（长度/字符集/短信模板）、type 定义、需要校验的业务 URL 规则。
 * <p>
 * 项目未引入 validation，无法用 @Validated，这里在启动期 fail-fast 校验。
 */
@Data
@ConfigurationProperties("captcha")
public class ValidateCodeProperties implements InitializingBean {

    /** 全局有效期（秒），type 可通过 expire-seconds 覆盖 */
    private long expireSeconds = 300;

    /** 短信签名 */
    private String signName;

    private Image image = new Image();

    private Sms sms = new Sms();

    /** type -> 验证码定义 */
    private Map<String, TypeSpec> types = new LinkedHashMap<>();

    /** 需要验证码校验的业务 URL */
    private List<Rule> rules = List.of();

    @Data
    public static class Image {
        private int length = 4;
        private int width = 120;
        private int height = 40;
        /** 候选字符，剔除易混淆的 0/O/1/I */
        private String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    }

    @Data
    public static class Sms {
        private int length = 6;
    }

    @Data
    public static class TypeSpec {
        private CodeKind kind;
        /** 短信模板 code（kind=sms 必填） */
        private String template;
        /** 本 type 有效期（秒），覆盖全局 */
        private Long expireSeconds;
        /** 发送前业务预检查：CodePreHandler 的 Spring Bean 名，未配置则不检查 */
        private String preHandler;
    }

    @Data
    public static class Rule {
        /** 网关原始路径（StripPrefix 之前），支持 PathPattern */
        private String url;
        /** 业务类型，默认取 url 最后一段 */
        private String type;
        /** 自定义 mobile 解析：MobileResolver 的 Spring Bean 名，未配置则走默认解析链 */
        private String mobile;

        public String effectiveType() {
            if (StringUtils.hasText(type)) {
                return type;
            }
            int idx = url == null ? -1 : url.lastIndexOf('/');
            return idx >= 0 && idx < url.length() - 1 ? url.substring(idx + 1) : url;
        }
    }

    @Override
    public void afterPropertiesSet() {
        boolean hasSms = false;
        for (Map.Entry<String, TypeSpec> entry : types.entrySet()) {
            TypeSpec spec = entry.getValue();
            Assert.notNull(spec.getKind(), "captcha.types." + entry.getKey() + ".kind 不能为空");
            if (spec.getKind() == CodeKind.SMS) {
                hasSms = true;
                Assert.hasText(spec.getTemplate(), "captcha.types." + entry.getKey() + ".template 不能为空");
            }
        }
        if (hasSms) {
            Assert.hasText(signName, "captcha.sign-name 不能为空");
        }
        for (Rule rule : rules) {
            Assert.hasText(rule.getUrl(), "captcha.rules[].url 不能为空");
            Assert.isTrue(types.containsKey(rule.effectiveType()),
                    "captcha.types 缺少 type 配置: " + rule.effectiveType());
        }
    }
}
