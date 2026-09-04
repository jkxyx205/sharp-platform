package com.rick.site.module.demo.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.site.module.demo.dao.PlantDAO;
import com.rick.site.module.demo.entity.Plant;
import org.springframework.stereotype.Service;

@Service
public class PlantService extends BaseCodeServiceImpl<PlantDAO, Plant, Long> {

    public PlantService(PlantDAO plantDAO) {
        super(plantDAO);
    }

}