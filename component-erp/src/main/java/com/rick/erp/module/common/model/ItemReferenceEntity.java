package com.rick.erp.module.common.model;

import com.rick.db.repository.model.BaseCodeEntity;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ItemReferenceEntity {

    Class<? extends BaseCodeEntity<Long>> mainEntity();

    Class<? extends BaseCodeEntity<Long>> itemEntity();
}