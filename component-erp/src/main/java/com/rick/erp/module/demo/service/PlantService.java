package com.rick.erp.module.demo.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.erp.module.demo.dao.PlantDAO;
import com.rick.erp.module.demo.entity.Plant;
import org.springframework.stereotype.Service;

@Service
public class PlantService extends BaseCodeServiceImpl<PlantDAO, Plant, Long> {

    public PlantService(PlantDAO plantDAO) {
        super(plantDAO);
    }

}