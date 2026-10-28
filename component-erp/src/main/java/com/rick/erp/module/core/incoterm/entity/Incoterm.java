package com.rick.erp.module.core.incoterm.entity;

import com.rick.db.repository.Table;
import com.rick.erp.module.common.model.PresetEntityIdCode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(value = "mm_incoterm", comment = "国际贸易术语")
public class Incoterm extends PresetEntityIdCode<Long> {

    @NotNull
    private Boolean locationRequired;
}