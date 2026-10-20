package com.rick.site.module.demo.controler;

import com.rick.api.platform.PlatformUserService;
import com.rick.common.component.starter.controller.BaseCodeApi;
import com.rick.common.component.starter.util.UserFillService;
import com.rick.site.module.demo.entity.MyEnum;
import com.rick.site.module.demo.service.MyEnumService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("my_enum")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MyEnumController extends BaseCodeApi<MyEnumService, MyEnum, Long> {

    // ❌ Avoid:
    // @DubboReference
    // private final DemoService demoService
    // @FieldDefaults(makeFinal = true) 会把字段变 final，Dubbo 无法注入，需 @NonFinal
    @NonFinal
    @DubboReference
    PlatformUserService platformUserService;

    public MyEnumController(MyEnumService baseService) {
        super(baseService);
    }

    @Override
    protected MyEnum getEntityFromOptional(Optional<MyEnum> optional, Object key) {
        MyEnum myEnum = optional.orElseThrow(() -> getResourceNotFoundException(key));

        // 用户表 fill
//        myEnum.getBaseEntityInfo().setCreateByName("Rick");
//        myEnum.getBaseEntityInfo().setUpdateByName("Rick");

        // 服务间调用
        Map<Long, String> idNicknameMap = platformUserService.getIdNicknameMap(List.of(myEnum.getBaseEntityInfo().getCreateBy(), myEnum.getBaseEntityInfo().getUpdateBy()));
//        myEnum.getBaseEntityInfo().setCreateByName(idNicknameMap.get(myEnum.getBaseEntityInfo().getCreateBy()));
//        myEnum.getBaseEntityInfo().setUpdateByName(idNicknameMap.get(myEnum.getBaseEntityInfo().getUpdateBy()));

        UserFillService.fillBaseEntityInfoGetterWithName(myEnum, idNicknameMap);
        return myEnum;
    }

    @PostMapping("valid")
    public void serviceValid(@RequestBody MyEnum myEnum) {
//        List<MyEnum> myEnumList = baseService.findByName(""); // 验证异常
        List<MyEnum> myEnumList = baseService.findByName("22");

        baseService.selectByCode("");  // 验证异常

        baseService.valid("", myEnum);  // 验证异常
    }


    /**
     *  public T patch(@PathVariable ID id, @Valid @RequestBody T t)
     *  不能重写 patch，否则 @Valid @RequestBody T t 还是会生效
     *  如果 t 中有对象，比如 DictValue materialType; 需要加上 @Valid，否则this.validatorHelper.validateProperty(entity, propertyName)
     *      报错：Invalid property path. Either there is no property materialType.code in entity com.rick.site.module.demo.entity.MyEnum or it is not possible to cascade to the property
     * @param id
     * @param t
     * @return
     */
    @PatchMapping("patch/{id}")
    public MyEnum patch2(@PathVariable Long id, @RequestBody MyEnum t) {
        t.setId(id);
        // 忽略null的更新，如果是 ""，标识需要更新，走检验逻辑
        t.setCode(null);
        t.setDescription(null);
        baseService.patch(t);
        return t;
    }

}
