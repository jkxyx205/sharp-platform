package com.rick.erp.module.inventory.entity;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.rick.db.repository.Column;
import com.rick.db.repository.Embedded;
import com.rick.db.repository.Table;
import com.rick.db.repository.model.BaseEntity;
import com.rick.erp.module.common.constant.DictValueSQLConstants;
import com.rick.erp.module.common.model.StockLocation;
import com.rick.meta.dict.model.DictType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(value = "inventory_stock", comment = "库存")
public
class Stock extends BaseEntity<Long> {

    @Embedded(comment = "库存八君子")
    @JsonUnwrapped
    StockLocation stockLocation;

    @With
    @NotNull
    BigDecimal quantity;

    /**
     * 库存单位，物料基本单位
     */
    @Column(updatable = false)
    @DictType(sql = DictValueSQLConstants.UNIT_DICT)
    String stockUnitCode;

}
