package com.rick.site.module.demo.controler;

import com.rick.common.component.starter.controller.BaseCodeApi;
import com.rick.site.module.demo.entity.MyEnum;
import com.rick.site.module.demo.service.MyEnumService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("my_enum")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MyEnumController extends BaseCodeApi<MyEnumService, MyEnum, Long> {


    public MyEnumController(MyEnumService baseService) {
        super(baseService);
    }

    @Override
    protected MyEnum getEntityFromOptional(Optional<MyEnum> optional, Object key) {
        MyEnum myEnum = optional.orElseThrow(() -> getResourceNotFoundException(key));

        // 用户表 fill
        myEnum.getBaseEntityInfo().setCreateByName("Rick");
        myEnum.getBaseEntityInfo().setUpdateByName("Rick");
        return myEnum;
    }

}
