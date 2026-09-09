package com.rick.common.component.starter.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.rick.db.repository.Column;
import com.rick.db.repository.model.BaseEntity;
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
public class ComponentBaseEntity<ID> extends BaseEntity<ID> implements GroupIdGetter {

    @Column(value = "group_id", updatable = false, comment = "所属公司id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;
}
