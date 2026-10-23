package com.rick.site.module.dog.controller;

import com.rick.common.component.starter.controller.BaseApi;
import com.rick.site.module.dog.entity.Dog;
import com.rick.site.module.dog.service.DogService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Rick.Xu
 * @date 2026-10-23 12:00:57
 */
@RestController
@RequestMapping("dogs")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DogController extends BaseApi<DogService, Dog, Long> {

    public DogController(DogService dogService) {
        super(dogService);
    }

}