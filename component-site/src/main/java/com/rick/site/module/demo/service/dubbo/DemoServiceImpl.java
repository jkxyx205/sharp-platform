package com.rick.site.module.demo.service.dubbo;

import com.rick.api.site.DemoService;
import com.rick.site.module.demo.entity.Plant;
import com.rick.site.module.demo.service.PlantService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class DemoServiceImpl implements DemoService {

    @Resource
    PlantService plantService;

    @Override
    public String sayHello(String name) {
        Plant plant = plantService.selectByCode("0001").get();
        return "Hello " + name + " " + plant.getDescription();
    }
}