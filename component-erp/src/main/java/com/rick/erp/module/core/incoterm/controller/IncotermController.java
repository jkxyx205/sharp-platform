package com.rick.erp.module.core.incoterm.controller;

import com.rick.common.component.starter.controller.BaseCodeApi;
import com.rick.erp.module.core.incoterm.entity.Incoterm;
import com.rick.erp.module.core.incoterm.service.IncotermService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Rick.Xu
 * @date 2026-10-27 09:45:14
 */
@RestController
@RequestMapping("incoterms")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IncotermController extends BaseCodeApi<IncotermService, Incoterm, Long> {

    public IncotermController(IncotermService incotermService) {
        super(incotermService);
    }

}