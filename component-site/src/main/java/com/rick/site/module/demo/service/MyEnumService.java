package com.rick.site.module.demo.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.site.module.demo.dao.MyEnumDAO;
import com.rick.site.module.demo.entity.MyEnum;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MyEnumService extends BaseCodeServiceImpl<MyEnumDAO, MyEnum, Long> {

    public MyEnumService(MyEnumDAO myEnumDAO) {
        super(myEnumDAO);
    }

}