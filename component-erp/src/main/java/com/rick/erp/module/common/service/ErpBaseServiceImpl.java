package com.rick.erp.module.common.service;

import com.rick.db.plugin.BaseServiceImpl;
import com.rick.db.repository.EntityDAO;
import com.rick.db.repository.model.EntityId;
import org.springframework.validation.annotation.Validated;

/**
 * @author Rick.Xu
 * @date 2025/11/15 07:55
 */
@Validated
public class ErpBaseServiceImpl<D extends EntityDAO<T, ID>, T extends EntityId<ID>, ID> extends BaseServiceImpl<D, T, ID> {

    public ErpBaseServiceImpl(D baseDAO) {
        super(baseDAO);
    }
}
