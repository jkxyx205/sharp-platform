package com.rick.erp.module.core.group.controller;

import com.rick.common.component.starter.controller.BaseCodeApi;
import com.rick.erp.module.core.group.entity.CodeDescription;
import com.rick.erp.module.core.group.service.CodeDescriptionService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Rick.Xu
 * @date 2026-10-27 17:17:14
 */
@RestController
@RequestMapping("codeDescriptions")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CodeDescriptionController extends BaseCodeApi<CodeDescriptionService, CodeDescription, Long> {

    public CodeDescriptionController(CodeDescriptionService codeDescriptionService) {
        super(codeDescriptionService);
    }

}