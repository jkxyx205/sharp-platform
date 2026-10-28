package com.rick.erp.module.core.group.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.erp.module.core.group.dao.CodeDescriptionDAO;
import com.rick.erp.module.core.group.entity.CodeDescription;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

/**
 * @author Rick.Xu
 * @date 2026-10-27 17:17:14
 */
@Service
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CodeDescriptionService extends BaseCodeServiceImpl<CodeDescriptionDAO, CodeDescription, Long> {

    public CodeDescriptionService(CodeDescriptionDAO codeDescriptionDAO) {
        super(codeDescriptionDAO);
    }

    public void insertOrUpdate(@NotNull CodeDescription.CategoryEnum category, Collection<CodeDescription> list) {
        baseDAO.insertOrUpdate(category, list, true, ids -> {
            // 判断是否允许删除
            System.out.println(ids);
        });
    }

    public List<CodeDescription> selectAll(CodeDescription.CategoryEnum category) {
        return baseDAO.selectAll(category);
    }
}