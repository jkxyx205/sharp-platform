package com.rick.erp.module.inventory.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.rick.common.component.starter.model.ComponentBaseEntity;
import com.rick.db.repository.ManyToOne;
import com.rick.db.repository.Table;
import com.rick.erp.module.material.entity.SerialNumber;
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
@Table(value = "mm_item_serial_number", comment = "Item 和 SerialNumber 关系表")
public class ItemSerialNumber extends ComponentBaseEntity<Long> {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;

    private String itemCode;

    @ManyToOne(value = "serial_number_id")
    private SerialNumber serialNumber;

    private String serialNumberCode;

}