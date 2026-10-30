package com.rick.erp.module.common.model;

import com.rick.db.repository.Column;
import com.rick.db.repository.OneToMany;
import com.rick.db.repository.support.CodeFillUncheck;
import com.rick.erp.module.inventory.entity.ItemSerialNumber;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * @author Rick.Xu
 * @date 2025/11/20 15:14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockLocation {

    @NotNull
    Long materialId;

    @NotBlank
    String materialCode;

    @OneToMany(joinColumnId = "item_id", mappedBy = "itemId")
    private List<ItemSerialNumber> itemSerialNumberList;

    Long batchId;

    @CodeFillUncheck
    String batchCode;

    @NotNull
    Long plantId;

    @NotBlank
    String plantCode;

    Long storageLocationId;

    String storageLocationCode;

    @Column(columnDefinition = "varchar(32)")
    SpecialStockEnum specialStock;

    String specialStockValue;

    @NotNull
    StockTypeEnum stockType;

//    @ManyToOne(value = "profile_id", comment = "profile 信息")
//    @JsonIgnore
//    private Profile profile;

//    @Transient
//    Long profileId;
}
