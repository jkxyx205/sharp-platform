package com.rick.demo.module.demo.controller;

import com.rick.common.component.starter.controller.BaseApi;
import com.rick.demo.module.demo.entity.Demo;
import com.rick.demo.module.demo.service.DemoService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 0. 接口文档地址：http://localhost:8769/api/demo/swagger-ui/index.html
 *
 * 1. 创建 Demo
 *   curl -X POST 'http://localhost:8769/api/demo/999/demos' \
 *   -H 'Content-Type: application/json' \
 *   -H 'Accept: application/json' \
 *   -H 'Authorization: Bearer 7e428776db02443b9d040a41c393b109' \
 *   -d '{
 *     "name": "demo"
 *   }'
 *
 *   response:
 *   {"id":"1146150534697193472","baseEntityInfo":{"createBy":"1","createTime":"2026-10-26 16:45:59","updateBy":"1","updateTime":"2026-10-26 16:45:59"},"groupId":"999","name":"demo"}
 *
 * 2. 根据 id 查询 Demo
 *   curl -X GET 'http://localhost:8769/api/demo/999/demos/1146150534697193472' \
 *   -H 'Content-Type: application/json' \
 *   -H 'Accept: application/json' \
 *   -H 'Authorization: Bearer 7e428776db02443b9d040a41c393b109'
 *   response:
 *   {"id":"1146150534697193472","baseEntityInfo":{"createBy":"1","createTime":"2026-10-26 16:45:59","updateBy":"1","updateTime":"2026-10-26 16:45:59"},"groupId":"999","name":"demo"}
 *
 * @author Rick.Xu
 * @date 2026-10-26 16:23:18
 */
@RestController
@RequestMapping("demos")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DemoController extends BaseApi<DemoService, Demo, Long> {

    public DemoController(DemoService demoService) {
        super(demoService);
    }

}