package com.rick.erp.module.core.unit.dao;

import com.rick.db.repository.EntityDAO;
import com.rick.erp.module.common.dao.PresetCategoryTypeEntityCodeDAOImpl;
import com.rick.erp.module.core.unit.entity.Dimension;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author Rick
 * @createdAt 2022-06-14 15:38:00
 */

@Repository
public class DimensionDAO extends PresetCategoryTypeEntityCodeDAOImpl<Dimension, Long> {

    @Override
    protected void itemDeletedCheckCallback(EntityDAO referenceDAO, Collection<Long> deletedIds) {
        super.itemDeletedCheckCallback(referenceDAO, deletedIds);
    }
}