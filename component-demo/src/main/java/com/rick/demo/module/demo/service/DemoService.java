package com.rick.demo.module.demo.service;

import com.rick.db.plugin.BaseServiceImpl;
import com.rick.demo.module.demo.dao.DemoDAO;
import com.rick.demo.module.demo.entity.Demo;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * @author Rick.Xu
 * @date 2026-10-26 16:23:18
 */
@Service
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DemoService extends BaseServiceImpl<DemoDAO, Demo, Long> {

    public DemoService(DemoDAO demoDAO) {
        super(demoDAO);
    }
}