package com.rick.site;

import com.rick.db.plugin.generator.TableGenerator;
import com.rick.site.module.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Rick.Xu
 * @date 2025/11/10 16:27
 */
// 自足运行：随机 Dubbo 端口 + 关闭 qos，避免与本机运行中的 site 实例（20880/22223）冲突；
// 不向 Nacos 注册测试实例，防止网关把流量路由到测试进程（订阅保留，@DubboReference 检查不受影响）
@SpringBootTest(properties = {
        "dubbo.protocol.port=-1",
        "dubbo.application.qos-enable=false",
        "dubbo.registry.register=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
public class TableGeneratorTest {

    @Autowired
    TableGenerator tableGenerator;

    // createTable 非幂等（表已存在会报错），按实体拆分方法，用 --tests 单方法执行

    @Test
    public void testGeneratorUserTable() {
        tableGenerator.createTable(User.class);
    }
}

