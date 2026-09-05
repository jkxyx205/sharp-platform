package com.rick.erp.module.demo.controler;

import com.rick.api.site.DemoService;
import com.rick.common.component.starter.controller.BaseCodeApi;
import com.rick.common.component.starter.model.User;
import com.rick.common.component.starter.model.UserContextHolder;
import com.rick.common.http.exception.BizException;
import com.rick.erp.module.common.exception.ExceptionCodeEnum;
import com.rick.erp.module.demo.entity.Plant;
import com.rick.erp.module.demo.service.PlantService;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("plants")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlantController extends BaseCodeApi<PlantService, Plant, Long> {

    // ❌ Avoid:
    // @DubboReference
    // private final DemoService demoService
    @DubboReference
    private DemoService demoService;

    @Resource
    PlantService plantService;

    public PlantController(PlantService baseService) {
        super(baseService);
    }

    @GetMapping("sites")
    public void getSites(User user) {
        User user2 = UserContextHolder.get();
        System.out.println(user);
        System.out.println(user2);
        throw new BizException(ExceptionCodeEnum.ERP_EXPIRED);
    }

    @GetMapping("current")
    public String getCurrentData() {
        Plant plant = plantService.selectByCode("C0013").get();
        return "Hello data" + plant.getDescription();
    }

    @GetMapping("dubbo")
    public String getRemoteData() {
        return demoService.sayHello("hello");
    }

}
