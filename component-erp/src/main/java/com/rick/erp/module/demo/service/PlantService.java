package com.rick.erp.module.demo.service;

import com.rick.api.site.DemoService;
import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.erp.module.demo.dao.PlantDAO;
import com.rick.erp.module.demo.entity.Plant;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlantService extends BaseCodeServiceImpl<PlantDAO, Plant, Long> {

    @DubboReference
    DemoService demoService;

    public PlantService(PlantDAO plantDAO) {
        super(plantDAO);
    }

    public String sayHello(String name) {
        return demoService.sayHello(name);
    }

}