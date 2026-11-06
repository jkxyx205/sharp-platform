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
        preProcess(entity); // 预处理
//        entityFieldsFillService.fill(entity); // fill 字段 id
        validate(entity); // 业务验证
        codeGenerate(entity); // 生成 code
        beforeInsertOrUpdate(entity);
        entity = super.insertOrUpdate(entity);
        afterInsertOrUpdate(entity);
        return entity;
    }

    /**
     * 钩子方法：数据预处理（如格式化、清洗）
     */
    protected void preProcess(T entity) {
    }

    /**
     * 钩子方法：业务验证
     */
    protected void validate(T entity) {

    }

    /**
     * 钩子方法：生成 code
     * @param entity
     */
    protected void codeGenerate(T entity) {

    }

    /**
     * 钩子方法：在实体保存之前执行
     * @param entity
     */
    protected void beforeInsertOrUpdate(T entity) {
    }

    /**
     * 钩子方法：在实体保存之后执行
     * @param entity
     */
    protected void afterInsertOrUpdate(T entity) {
    }
}