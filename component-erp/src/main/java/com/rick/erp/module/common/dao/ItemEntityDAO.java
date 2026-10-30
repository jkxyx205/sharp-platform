package com.rick.erp.module.common.dao;

import com.rick.db.repository.model.EntityIdCode;
import com.rick.db.repository.support.category.CategoryEntityCodeDAOImpl;
import com.rick.db.repository.support.category.RowCategory;
import com.rick.erp.module.common.constant.HeaderConstants;

public class ItemEntityDAO<T extends EntityIdCode<Long> & RowCategory<String>> extends CategoryEntityCodeDAOImpl<T, Long, String> {

    public ItemEntityDAO() {
        super(HeaderConstants.HEADER_CODE_COLUMN_NAME);
    }

}
