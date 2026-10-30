package com.rick.erp.module.material.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.rick.db.repository.Column;
import com.rick.db.repository.Select;
import com.rick.db.repository.Table;
import com.rick.db.repository.model.BaseCodeEntity;
import com.rick.db.repository.support.category.RowCategory;
import com.rick.erp.module.inventory.entity.Stock;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Objects;


/**
 * 序列号 + materialId =  主键
 * @author Rick
 * @createdAt 2023-02-23 16:58:00
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(value = "mm_serial_number", comment = "序列号")
public class SerialNumber extends BaseCodeEntity<Long> implements RowCategory<String> {

    @Column(updatable = false)
    private Long materialId;

    @NotBlank
    @Column(updatable = false)
    private String materialCode;

    private SerialNumberEnum status;

//    private Long batchId;
//
//    private String batchCode;

    /**
     * 库存位置信息
     */
    @Select(value = "select material_id AS \"stockLocation.materialId\", material_code AS \"stockLocation.materialCode\", batch_id AS \"stockLocation.batchId\", batch_code AS \"stockLocation.batchCode\", plant_id AS \"stockLocation.plantId\", plant_code AS \"stockLocation.plantCode\", storage_location_id AS \"stockLocation.storageLocationId\", storage_location_code AS \"stockLocation.storageLocationCode\", special_stock AS \"stockLocation.specialStock\", special_stock_value AS \"stockLocation.specialStockValue\", stock_type AS \"stockLocation.stockType\", quantity AS \"quantity\", stock_unit_code AS \"stockUnitCode\", mm_item_serial_number.create_by AS \"baseEntityInfo.createBy\", mm_item_serial_number.create_time AS \"baseEntityInfo.createTime\", mm_item_serial_number.update_by AS \"baseEntityInfo.updateBy\", mm_item_serial_number.update_time AS \"baseEntityInfo.updateTime\", mm_item_serial_number.is_deleted AS \"baseEntityInfo.deleted\", mm_item_serial_number.id AS \"id\" from mm_item_serial_number, inventory_stock" +
            " where serial_number_id = :serialNumberId and item_id = inventory_stock.id", params = "serialNumberId@id", nullWhenParamsIsNull = "id")
    private Stock stock;

    /**
     // 获取序列号物料的库存信息
     select inventory_stock.* from mm_item_serial_number, inventory_stock
     where serial_number_id = 1072670447541161984 and item_id = inventory_stock.id

     // 获取序列号物料库存对应的序列号数据
     select mm_serial_number.* from mm_item_serial_number, inventory_stock, mm_serial_number
     where inventory_stock.id = 1105347033515999232
     and item_id = inventory_stock.id and mm_serial_number.id = mm_item_serial_number.serial_number_id
     * @return
     */
    // 参考 Stock ID，出库后，状态变离库，取消对 Stock ID 的引用
    // 物料凭证历史
    // 序列号物料库存报表，可以点击查看详情

    @Override
    public String getCategory() {
        return this.materialCode;
    }

    @Override
    public void setCategory(String materialCode) {
        this.materialCode = materialCode;
    }
    @AllArgsConstructor
    @Getter
    public enum SerialNumberEnum {
        CREATED("创建"),
        AVAILABLE("在库"),
        SOLD("离库");

        @JsonValue
        public String getCode() {
            return this.name();
        }

        private final String label;

        public static SerialNumberEnum valueOfCode(String code) {
            return valueOf(code);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SerialNumber)) {
            return false;
        }
        SerialNumber other = (SerialNumber) o;
        if (!other.canEqual(this)) {
            return false;
        }
        // code 来自父类,通过 getter 获取
        Object thisCode = this.getCode();
        Object otherCode = other.getCode();
        if (!Objects.equals(thisCode, otherCode)) {
            return false;
        }
        // materialCode 是当前类自己的字段
        return Objects.equals(this.materialCode, other.materialCode);
    }

    protected boolean canEqual(Object other) {
        return other instanceof SerialNumber;
    }

    @Override
    public int hashCode() {
        // 用 code(父类) + materialCode(本类) 一起计算哈希
        return Objects.hash(getCode(), materialCode);
    }

}
