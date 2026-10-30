package com.rick.erp.module.common.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.db.repository.EntityCodeDAO;
import com.rick.db.repository.model.EntityIdCode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
public class ErpBaseCodeServiceImpl<D extends EntityCodeDAO<T, ID>, T extends EntityIdCode<ID>, ID> extends BaseCodeServiceImpl<D, T, ID> {

    public ErpBaseCodeServiceImpl(D baseDAO) {
        super(baseDAO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public T insertOrUpdate(T entity) {
        codeGenerate(entity);
        beforeInsertOrUpdate(entity);
        entity = super.insertOrUpdate(entity);
        return entity;
    }

    protected void beforeInsertOrUpdate(T entity) {
    }

    protected void codeGenerate(T entity) {

    }
}