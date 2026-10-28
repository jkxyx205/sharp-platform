package com.rick.erp.module.common.dao;

import com.rick.common.http.exception.ExceptionCode;
import com.rick.db.repository.model.EntityIdCode;
import com.rick.db.repository.support.ConditionEntityCodeDAOImpl;
import com.rick.db.util.OperatorUtils;
import com.rick.erp.module.common.model.PresetEntityIdCode;
import com.rick.erp.module.common.model.PresetTypeEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 只能修改自定义信息
 * 固定字段 category_type：
 *     PRESET("系统预设"),
 *     CUSTOMIZE("客户自定义");
 * @author Rick.Xu
 * @date 2026/11/19 12:06
 */
public class PresetCategoryTypeEntityCodeDAOImpl<T extends PresetEntityIdCode<ID>, ID> extends ConditionEntityCodeDAOImpl<T, ID> {

    protected String getMergeArgsCondition(String condition) {
        return formatConditionConnect(condition) + "category_type = ?";
    }

    protected Object[] getMergeArgs(Object[] args) {
        return ArrayUtils.addAll(args, PresetTypeEnum.CUSTOMIZE.getCode());
    }

    protected String getMergeMapCondition(String condition) {
        return formatConditionConnect(condition) + "category_type = :categoryType";
    }

    protected Map<String, Object> getMergeMap(Map<String, Object> paramMap) {
        Map<String, Object> mergeMap = new HashMap<>(paramMap);
        mergeMap.put("categoryType", PresetTypeEnum.CUSTOMIZE.getCode());
        return mergeMap;
    }

    @Override
    protected <E> List<E> select(Class<E> clazz, String sql, Map<String, Object> paramMap) {
        return super.select(clazz, sql + " AND (group_id = 0 or group_id = :group_id)", paramMap);
    }

    @Override
    public <E> List<E> select(Class<E> clazz, String columns, String condition, Object... args) {
        return super.select(clazz, columns, formatConditionConnect(condition) + "(group_id = 0 or group_id = ?)" , args);
    }

    @Override
    public boolean exists(String condition, Object... args) {
        return super.exists(formatConditionConnect(condition) + "(group_id = 0 or group_id = ?)", args);
    }

    @Override
    public boolean exists(String condition, Map<String, Object> paramMap) {
        return super.exists(formatConditionConnect(condition) + "(group_id = 0 or group_id = :group_id)", paramMap);
    }

    public List<T> insertOrUpdate(List<T> itemList, Consumer<Collection<ID>> deletedIdsConsumer) {
        if (CollectionUtils.isEmpty(itemList)) {
            deleteAll();
        } else {
            List<String> codeList = itemList.stream().map(T::getCode).collect(Collectors.toList());
            // code 不能重复
            ExceptionCode.state(!OperatorUtils.hasDuplicates(codeList), "code 不能重复");

            // code 不能和预设的冲突
            ExceptionCode.state(!exists("category_type = 'PRESET' AND code IN (:codes)", Map.of("codes", codeList)), "code 不能和预设的重复");

            fillEntityIdsByCodes(itemList);
            List<ID> deleteIds = select(getTableMeta().getIdMeta().idClass(), "id", "code NOT IN (:codes) AND category_type = 'CUSTOMIZE' AND is_deleted = false",
                    Map.of("codes", itemList.stream().map(T::getCode).collect(Collectors.toSet())));

            if (Objects.nonNull(deletedIdsConsumer)) {
                deletedIdsConsumer.accept(deleteIds);
            }

            if (CollectionUtils.isEmpty(deleteIds)) {
                deleteAll();
            } else {
                deleteByIds(deleteIds);
            }

//            delete("code NOT IN (:codes) AND category_type = 'CUSTOMIZE' AND is_deleted = false",
//                    Map.of("codes", itemList.stream().map(T::getCode).collect(Collectors.toSet())));

            insertOrUpdate(itemList);
        }

        return itemList;
    }


    private String formatConditionConnect(String condition) {
        if (StringUtils.isBlank(condition)) {
            return "";
        }

        return condition + " AND ";
    }

    private void fillEntityIdsByCodes(Collection<T> entities) {
        if (CollectionUtils.isNotEmpty(entities)) {
            Set<String> emptyIdCodeSet = entities.stream().filter(t -> Objects.isNull(t.getId())).map(EntityIdCode::getCode).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(emptyIdCodeSet)) {
                Map<String, ID> codeIdMap = selectCodeIdMap(emptyIdCodeSet);

                if (MapUtils.isNotEmpty(codeIdMap)) {
                    for (T t : entities) {
                        // fillIds
                        if (codeIdMap.containsKey(t.getCode())) {
                            t.setId(codeIdMap.get(t.getCode()));
                        }
                        t.setCategoryType(PresetTypeEnum.CUSTOMIZE);
                    }
                }
            }
        }
    }

}
