package com.rick.erp.module.common.dao;

import com.rick.db.repository.model.EntityIdCode;
import com.rick.db.repository.support.ConditionEntityCodeDAOImpl;
import com.rick.erp.module.common.model.PresetTypeEnum;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 只能修改自定义信息
 * 固定字段 category_type：
 *     PRESET("系统预设"),
 *     CUSTOMIZE("客户自定义");
 * @author Rick.Xu
 * @date 2026/11/19 12:06
 */
public class PresetCategoryTypeEntityCodeDAOImpl<T extends EntityIdCode<ID>, ID> extends ConditionEntityCodeDAOImpl<T, ID> {

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

    private String formatConditionConnect(String condition) {
        if (StringUtils.isBlank(condition)) {
            return "";
        }

        return condition + " AND ";
    }

}
