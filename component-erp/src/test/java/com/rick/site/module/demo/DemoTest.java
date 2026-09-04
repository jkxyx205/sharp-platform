package com.rick.erp.module.demo;

import com.rick.erp.BaseTest;
import com.rick.erp.module.demo.entity.Plant;
import com.rick.erp.module.demo.service.PlantService;
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
public class DemoTest extends BaseTest<PlantService, Plant, Long>  {

    public DemoTest(@Autowired PlantService baseService) {
        super(baseService);
    }

    @Test
    public void testSelectByCode() {
        Optional<Plant> optional = baseService.selectByCode("0001");
        assertEquals(true, optional.isPresent());
        Plant plant = optional.get();
        assertEquals("库房0001", plant.getDescription());
        log.info(plant.getDescription());
    }
}
