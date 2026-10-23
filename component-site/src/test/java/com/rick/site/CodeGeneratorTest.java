package com.rick.site;

import com.rick.db.repository.TableDAO;
import com.rick.generator.Generator;
import com.rick.site.module.dog.entity.Dog;
import com.rick.site.module.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;

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
public class CodeGeneratorTest {

    @Autowired
    private Generator generator;

    @Autowired
    private TableDAO tableDAO;

    // createTable 非幂等（表已存在会报错），按实体拆分方法，用 --tests 单方法执行

    @Test
    public void testGeneratorDogTable() throws IOException {
        tableDAO.execute("DROP TABLE IF EXISTS t_dog");

        generator.execute(Dog.class,
                "/Users/rick/Space/Workspace/sharp-platform/component-site/src/main/java/com/rick/site/module/dog",
                Map.of(
                        Generator.PROJECT, "component-site",
                        Generator.REPORT, false
                ));
    }

    @Test
    public void testGeneratorUserTable() throws IOException {
//        tableDAO.execute("DROP TABLE IF EXISTS sys_user");

        generator.execute(User.class,
                "/Users/rick/Space/Workspace/sharp-platform/component-site/src/main/java/com/rick/site/module/user",
                Map.of(
                        Generator.PROJECT, "component-site",
                        Generator.REPORT, false,
                        Generator.FORM_PAGE, "demos/user/edit-thymeleaf" // 编辑页面路径
                ));
    }

    @Test
    public void testGeneratorUserTableFull() throws IOException {
//        tableDAO.execute("DROP TABLE IF EXISTS sys_user");

        generator.execute(User.class,
                "/Users/rick/Space/Workspace/sharp-platform/component-site/src/main/java/com/rick/site/module/sys_user",
                Map.of(
//                        ,Generator.GENERATOR_CODE, true) // 是否创建 code template；如果没有指定，则没有就创建，否则不覆盖
                        Generator.PROJECT, "component-site",
                        Generator.FORM_PAGE, "demos/user/edit-thymeleaf",// 编辑页面路径

                        Generator.REPORT, false, // 是否创建 report
//                        Generator.REPORT_TEST_PATH, "/Users/rick/Space/Workspace/sharp-project/sharp-admin/src/test/java/com/rick/admin/demo",
//                        Generator.REPORT_TEST_PACKAGE, "com.rick.admin.demo",

                        // 表名默认 control.html
//                        Generator.CONTROL_PATH, "/Users/rick/Space/Workspace/sharp-project/sharp-admin/src/main/resources/templates/demos/user",
                        Generator.CONTROL_LABEL, false
//                        Generator.FORM_LAYOUT, FormLayoutEnum.HORIZONTAL)
        )); // 默认就是 HORIZONTAL
//                        ,Generator.CONTROL_RENDER_TYPE, RenderTypeEnum.THYMELEAF)
    }
}


