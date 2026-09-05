package com.rick.site.module.demo.service.dubbo;

import com.rick.api.site.DemoService;
import com.rick.site.module.demo.entity.Plant;
import com.rick.site.module.demo.service.PlantService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 有 @DubboService 才不会是空服务
 */
@DubboService
public class DemoServiceImpl implements DemoService {

    @Resource
    PlantService plantService;

    @Override
    public String sayHello(String name) {
        Plant plant = plantService.selectByCode("C0013").get();
        return "Hello " + name + " " + plant.getDescription();
    }
}