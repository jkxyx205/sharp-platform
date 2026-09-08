package com.rick.gateway.captcha;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 图片验证码：按 type 获取（type 须配置为 kind=image），渲染 PNG 返回。
 * <p>
 * 存储 key 为 deviceId:type（deviceId 请求头），后续业务 URL 由 {@link ValidateCodeFilter} 用同一 key 校验。
 */
@RestController
@RequestMapping("image")
public class ImageCodeController {

    private final ValidateCodeService service;
    private final ValidateCodeProperties properties;
    private final SecureRandom random = new SecureRandom();

    public ImageCodeController(ValidateCodeService service, ValidateCodeProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("login")
    public Mono<ResponseEntity<Resource>> loginImage(@RequestHeader(value = "deviceId", required = false) String deviceId) {
        return image("login", deviceId);
    }

    @GetMapping
    public Mono<ResponseEntity<Resource>> image(@RequestParam String type,
                                                @RequestHeader(value = "deviceId", required = false) String deviceId) {
        // 渲染为 CPU 阻塞操作，放到 boundedElastic，勿占事件循环
        return Mono.fromCallable(() -> {
                    if (!StringUtils.hasText(deviceId)) {
                        throw new IllegalArgumentException("缺少设备标识 deviceId");
                    }
                    if (service.typeSpec(type).getKind() != CodeKind.IMAGE) {
                        throw new IllegalArgumentException("type 不是图片验证码: " + type);
                    }
                    ValidateCode code = service.sendCode(type, null, deviceId);
                    return ResponseEntity.ok()
                            .contentType(MediaType.IMAGE_PNG)
                            .cacheControl(CacheControl.noStore())
                            .body((Resource) new ByteArrayResource(render(code.content())));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(error(e)));
    }

    private static ResponseEntity<Resource> error(Throwable e) {
        int status = e instanceof IllegalArgumentException ? 400 : 500;
        String message = e instanceof IllegalArgumentException ? e.getMessage() : "图片验证码生成失败";
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ByteArrayResource(
                        CaptchaJson.error(status, message).getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 把验证码文本渲染成 PNG：逐字符随机颜色/旋转 + 干扰线。
     */
    private byte[] render(String content) throws IOException {
        int width = properties.getImage().getWidth();
        int height = properties.getImage().getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        for (int i = 0; i < 5; i++) {
            g.setColor(randomColor(160, 220));
            g.drawLine(random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height));
        }

        // 逻辑字体，避免容器/无字体环境缺 Arial
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, (int) (height * 0.7)));
        int step = width / (content.length() + 1);
        for (int i = 0; i < content.length(); i++) {
            g.setColor(randomColor(20, 130));
            int x = step / 2 + i * step + random.nextInt(4);
            int y = (int) (height * 0.75) + random.nextInt(5) - 2;
            double angle = (random.nextDouble() - 0.5) * 0.5;
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(content.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }
}
