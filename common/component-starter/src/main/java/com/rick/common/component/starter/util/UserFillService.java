package com.rick.common.component.starter.util;

import com.rick.db.repository.model.BaseEntityInfo;
import com.rick.db.repository.model.BaseEntityInfoGetter;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@UtilityClass
public class UserFillService {

    public void fillBaseEntityInfoGetterWithName(BaseEntityInfoGetter baseEntityInfoGetter, Map<Long, String> idNicknameMap) {
        if (Objects.isNull(baseEntityInfoGetter)) {
            return;
        }

        fillBaseEntityInfoGetterWithName(Set.of(baseEntityInfoGetter), idNicknameMap);
    }

    public void fillBaseEntityInfoGetterWithName(Collection<BaseEntityInfoGetter> baseEntityInfoGetterCollection, Map<Long, String> idNicknameMap) {
        if (CollectionUtils.isEmpty(baseEntityInfoGetterCollection) || MapUtils.isEmpty(idNicknameMap)) {
            return;
        }

        for (BaseEntityInfoGetter baseEntityInfoGetter : baseEntityInfoGetterCollection) {
            BaseEntityInfo baseEntityInfo = baseEntityInfoGetter.getBaseEntityInfo();
            if (Objects.nonNull(baseEntityInfo)) {
                baseEntityInfo.setCreateByName(idNicknameMap.get(baseEntityInfo.getCreateBy()));
                baseEntityInfo.setUpdateByName(idNicknameMap.get(baseEntityInfo.getUpdateBy()));
            }
        }
    }

}
