package com.rick.erp.module.common.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.rick.db.repository.Embedded;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;


/**
 * @author Rick.Xu
 * @date 2026/5/4 08:13
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
public abstract class StockItemEntity extends ItemEntity {

    @Embedded(comment = "库存八君子")
    @JsonUnwrapped
    StockLocation stockLocation;

    // 以下是附加值

    /**
     * 物料基本信息
     */
//    @Select(entityClass = Material.class, // select
//            value = "WHERE id = :id", // condition
//            params = "id@stockLocation.materialId", nullWhenParamsIsNull = "id", cascadeSelect = false)
//    Material material;

    /**
     * 特征值
     */
//    @Select(entityClass = Classification.class,
//            value = "WHERE EXISTS (select 1 from mm_material_classification WHERE classification_id = mm_classification.id AND material_id = :id)",
//            params = "id@stockLocation.materialId", nullWhenParamsIsNull = "id")
//    List<Classification> classificationList;

    public abstract BoundTypeEnum getBoundType();

}