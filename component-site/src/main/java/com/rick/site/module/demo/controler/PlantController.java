package com.rick.site.module.demo.controler;

import com.rick.common.component.starter.controller.BaseCodeApi;
import com.rick.common.http.exception.BizException;
import com.rick.site.module.common.exception.ExceptionCodeEnum;
import com.rick.site.module.demo.entity.Plant;
import com.rick.site.module.demo.service.PlantService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("plants")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlantController extends BaseCodeApi<PlantService, Plant, Long> {

    public PlantController(PlantService baseService) {
        super(baseService);
    }

    @GetMapping("sites")
    public void getSites(@PathVariable String groupId) {
        System.out.println(groupId);
        throw new BizException(ExceptionCodeEnum.SITE_EXPIRED);
    }

}
