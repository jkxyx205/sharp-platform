package com.rick.erp.module.core.incoterm.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.erp.module.common.model.PresetTypeEnum;
import com.rick.erp.module.core.incoterm.dao.IncotermDAO;
import com.rick.erp.module.core.incoterm.entity.Incoterm;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (CollectionUtils.isEmpty(incotermList)) {
            baseDAO.deleteAll();
        } else {
            // TODO
            // code 不能重复
            // code 不能和预设的冲突
//            baseDAO.assertCodesUnDuplicate(incotermList.stream().map(Incoterm::getCode).collect(Collectors.toList()));
            Map<String, Long> codeIdMap = baseDAO.selectCodeIdMap(incotermList.stream().map(Incoterm::getCode).collect(Collectors.toSet()));
            for (Incoterm incoterm : incotermList) {
//                if (!incoterm.getCode().startsWith(SysConstants.CUSTOMIZE_CODE_PREFIX)) {
//                    throw new BizException(ExceptionCodeEnum.CODE_NOT_Z_ERROR);
//                }
                incoterm.setId(codeIdMap.get(incoterm.getCode()));
                incoterm.setCategoryType(PresetTypeEnum.CUSTOMIZE);
            }

            // TODO 检查是否允许被删除
            baseDAO.delete("code NOT IN (:codes) AND category_type = 'CUSTOMIZE'",
                    Map.of("codes", incotermList.stream().map(Incoterm::getCode).collect(Collectors.toSet())));
            baseDAO.insertOrUpdate(incotermList);
        }

        return incotermList;
    }
}