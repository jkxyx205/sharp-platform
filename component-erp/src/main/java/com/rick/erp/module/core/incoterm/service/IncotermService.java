package com.rick.erp.module.core.incoterm.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.erp.module.core.incoterm.dao.IncotermDAO;
import com.rick.erp.module.core.incoterm.entity.Incoterm;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @author Rick.Xu
 * @date 2026-10-27 09:45:14
 */
@Service
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IncotermService extends BaseCodeServiceImpl<IncotermDAO, Incoterm, Long> {

    public IncotermService(IncotermDAO incotermDAO) {
        super(incotermDAO);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Incoterm> saveOrUpdate(List<Incoterm> incotermList) {
        return baseDAO.insertOrUpdate(incotermList, ids -> {
            // 判断是否允许删除
            System.out.println(ids);
        });
    }
}