package com.rick.erp.module.core.unit.service;

import com.rick.erp.module.common.service.ErpBaseCodeServiceImpl;
import com.rick.erp.module.core.unit.dao.DimensionDAO;
import com.rick.erp.module.core.unit.entity.Dimension;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author Rick.Xu
 * @date 2024/8/23 16:49
 */
@Service
public class DimensionService extends ErpBaseCodeServiceImpl<DimensionDAO, Dimension, Long> {

    public DimensionService(DimensionDAO dimensionDAO) {
        super(dimensionDAO);
    }

    public Optional<Dimension> selectByCode(String code) {
        return baseDAO.selectByCode(code);
    }

    public List<Dimension> saveOrUpdate(List<Dimension> dimensionList) {
//        if (CollectionUtils.isNotEmpty(dimensionList)) {
//            for (Dimension dimension : dimensionList) {
//                dimension.setCategoryType(PresetTypeEnum.CUSTOMIZE);
//            }
//            entityFieldsFillService.fill(dimensionList);
//        }
//
//        baseDAO.insertOrUpdate(dimensionList, ids -> {
//
//        });

//        super.insertOrUpdateTable(dimensionList);
        return dimensionList;
    }
}