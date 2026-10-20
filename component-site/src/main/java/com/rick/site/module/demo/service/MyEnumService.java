package com.rick.site.module.demo.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.site.module.demo.dao.MyEnumDAO;
import com.rick.site.module.demo.entity.MyEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MyEnumService extends BaseCodeServiceImpl<MyEnumDAO, MyEnum, Long> {

    public MyEnumService(MyEnumDAO myEnumDAO) {
        super(myEnumDAO);
    }

    public void valid(@NotBlank String code, @Valid MyEnum myEnum) {
        System.out.println("valid execute...");
    }

    public List<MyEnum> findByName(String name) {
        return baseDAO.findByName(name);
    }

}