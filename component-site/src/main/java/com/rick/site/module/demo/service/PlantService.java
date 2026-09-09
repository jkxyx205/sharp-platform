package com.rick.site.module.demo.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.site.module.demo.dao.PlantDAO;
import com.rick.site.module.demo.entity.Plant;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlantService extends BaseCodeServiceImpl<PlantDAO, Plant, Long> {

    public PlantService(PlantDAO plantDAO) {
        super(plantDAO);
    }

}