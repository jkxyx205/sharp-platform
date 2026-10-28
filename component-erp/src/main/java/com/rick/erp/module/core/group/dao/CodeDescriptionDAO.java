package com.rick.erp.module.core.group.dao;

import com.rick.db.repository.support.category.CategoryEnumEntityCodeDAOImpl;
import com.rick.erp.module.core.group.entity.CodeDescription;
import org.springframework.stereotype.Repository;

/**
 * @author Rick.Xu
 * @date 2026-10-27 17:17:14
 */
@Repository
public class CodeDescriptionDAO extends CategoryEnumEntityCodeDAOImpl<CodeDescription, Long, CodeDescription.CategoryEnum> {

}