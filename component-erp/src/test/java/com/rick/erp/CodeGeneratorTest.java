package com.rick.erp;

import com.rick.db.repository.TableDAO;
import com.rick.erp.module.core.incoterm.entity.Incoterm;
import com.rick.generator.Generator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;

/**
 * @author Rick.Xu
 * @date 2025/11/10 16:27
 */
// 自足运行：随机 Dubbo 端口 + 关闭 qos，避免与本机运行中的 demo 实例（20882/22224）冲突；
// 不向 Nacos 注册测试实例，防止网关把流量路由到测试进程（订阅保留，@DubboReference 检查不受影响）
@SpringBootTest(properties = {
        "dubbo.protocol.port=-1",
        "dubbo.application.qos-enable=false",
        "dubbo.registry.register=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
public class CodeGeneratorTest {

    @Autowired
    private Generator generator;

    @Autowired
    private TableDAO tableDAO;

    // createTable 非幂等（表已存在会报错），按实体拆分方法，用 --tests 单方法执行

    @Test
    public void testGeneratorDemoTable() throws IOException {
//        tableDAO.execute("DROP TABLE IF EXISTS t_demo");

        generator.execute(Incoterm.class,
                "/Users/rick/Space/Workspace/sharp-platform/component-erp/src/main/java/com/rick/erp/module/core/incoterm",
                Map.of(
                        Generator.CREATE_TABLE, false,
                        Generator.PROJECT, "component-erp",
                        Generator.REPORT, false
                ));
    }

}


