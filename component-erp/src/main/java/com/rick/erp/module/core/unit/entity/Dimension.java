package com.rick.erp.module.core.unit.entity;

import com.rick.db.repository.OneToMany;
import com.rick.db.repository.Table;
import com.rick.erp.module.common.model.PresetEntityIdCode;
import com.rick.erp.module.common.model.PresetTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Objects;

/**
 * @author Rick
 * @createdAt 2022-06-14 15:32:00
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(value = "mm_unit_dimension", comment = "单位纬度")
public class Dimension extends PresetEntityIdCode<Long> {

    @OneToMany(cascadeSave = false, joinColumnId = "dimension_id", mappedBy = "dimensionId")
    private List<Unit> unitList;

    public Boolean isCustomizeDimension() {
        return super.getCategoryType() == PresetTypeEnum.CUSTOMIZE;
    }

    public Unit getBaseUnit() {
        return unitList.stream().filter(unit -> Objects.equals(unit.getN(), unit.getD())).findFirst().get();
    }

}