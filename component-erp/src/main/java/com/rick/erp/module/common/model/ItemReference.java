package com.rick.erp.module.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * @author Rick.Xu
 * @date 2026/04/20 15:14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemReference {

    @JsonProperty("Id")
    Long id;

    @JsonProperty("Code")
    String code;

    @JsonProperty("ItemId")
    Long itemId;

    @JsonProperty("ItemCode")
    String itemCode;

}
