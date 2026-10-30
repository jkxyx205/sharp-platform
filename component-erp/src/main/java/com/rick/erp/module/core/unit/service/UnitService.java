package com.rick.erp.module.core.unit.service;

import com.rick.erp.module.common.service.ErpBaseCodeServiceImpl;
import com.rick.erp.module.core.unit.dao.UnitDAO;
import com.rick.erp.module.core.unit.entity.Unit;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Rick.Xu
 * @date 2024/8/23 16:49
 */
@Service
public class UnitService extends ErpBaseCodeServiceImpl<UnitDAO, Unit, Long> {

    public UnitService(UnitDAO unitDAO) {
        super(unitDAO);
    }

    public List<Unit> saveOrUpdate(String dimensionCode, List<Unit> unitList) {
//        if (CollectionUtils.isNotEmpty(unitList)) {
//            for (Unit unit : unitList) {
//                unit.setDimensionCode(dimensionCode);
//                unit.setCategoryType(PresetTypeEnum.CUSTOMIZE);
//            }
//            entityFieldsFillService.fill(unitList);
//        }

        // TODO insertOrUpdate 如果 unit code 在 dimension 唯一，那么需要将 dimension_code 作为查询条件。现实中整个系统 unit code 唯一
        baseDAO.insertOrUpdate(unitList, ids -> {

        });
//        super.insertOrUpdateTable(unitList, "dimension_code", dimensionCode);
        return unitList;
    }
}