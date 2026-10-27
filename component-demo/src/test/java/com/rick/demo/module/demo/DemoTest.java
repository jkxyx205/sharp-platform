package com.rick.demo.module.demo;

import com.rick.demo.BaseTest;
import com.rick.demo.module.demo.entity.Demo;
import com.rick.demo.module.demo.service.DemoService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DemoTest extends BaseTest<DemoService, Demo, Long> {

    public DemoTest(@Autowired DemoService baseService) {
        super(baseService);
    }

    @Test
    public void testInsert() {
        Demo demo = baseService.insert(Demo.builder().name("demo").build());
    }

    @Test
    public void testSelectById() {
        Optional<Demo> optional = baseService.selectById(1146145604653551616L);
        assertEquals(true, optional.isPresent());
        Demo demo = optional.get();
        assertEquals("demo", demo.getName());
        log.info(demo.getName());
    }
}
