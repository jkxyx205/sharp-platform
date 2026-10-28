package com.rick.erp.module.common.model;

import com.rick.common.component.starter.model.ComponentBaseCodeDescriptionEntity;
import com.rick.db.repository.Column;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * @author Rick.Xu
 * @date 2026/10/27 19:34
 */
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresetEntityIdCode<ID> extends ComponentBaseCodeDescriptionEntity<ID> {

    @Column(updatable = false)
    private PresetTypeEnum categoryType;
}
