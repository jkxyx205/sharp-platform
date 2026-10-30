package com.rick.erp.module.core.unit.entity;


import com.rick.db.repository.Table;
import com.rick.erp.module.common.model.PresetEntityIdCode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 单位
 * @author Rick
 * @createdAt 2022-06-14 15:23:00
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(value = "mm_unit", comment = "单位")
public class Unit extends PresetEntityIdCode<Long> {

    @NotNull(message = "分子不能为空")
    private Integer n;

    @NotNull(message = "分母不能为空")
    private Integer d;

    private Integer e;

    private BigDecimal k;

    @NotNull(message = "维度id不能为空")
    private Long dimensionId;

    @NotNull(message = "维度code不能为空")
    private String dimensionCode;

    public Boolean isBaseUnit() {
        return Objects.equals(n, d);
    }
}