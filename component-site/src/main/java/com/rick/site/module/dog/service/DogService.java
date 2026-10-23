package com.rick.site.module.dog.service;

import com.rick.db.plugin.BaseServiceImpl;
import com.rick.site.module.dog.dao.DogDAO;
import com.rick.site.module.dog.entity.Dog;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * @author Rick.Xu
 * @date 2026-10-23 12:00:57
 */
@Service
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DogService extends BaseServiceImpl<DogDAO, Dog, Long> {

    public DogService(DogDAO dogDAO) {
        super(dogDAO);
    }
}