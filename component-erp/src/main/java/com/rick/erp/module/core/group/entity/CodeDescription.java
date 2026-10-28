package com.rick.erp.module.core.group.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rick.common.component.starter.model.ComponentBaseCodeDescriptionEntity;
import com.rick.common.util.model.IEnum;
import com.rick.db.repository.Table;
import com.rick.db.repository.support.CodeFillUncheck;
import com.rick.db.repository.support.category.RowCategory;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "sys_code_description", comment = "编号-描述")
@CodeFillUncheck
public class CodeDescription extends ComponentBaseCodeDescriptionEntity<Long> implements RowCategory<CodeDescription.CategoryEnum> {

    @NotNull
    CategoryEnum category;

    Integer sort;

    @AllArgsConstructor
    @Getter
    @JsonFormat(shape = JsonFormat.Shape.OBJECT)
    public enum CategoryEnum implements IEnum<String> {
        /**
         * 物料组
         */
        MATERIAL("物料组"),
        /**
         * 采购组织
         */
        PURCHASING_ORG("采购组织"),
        /**
         * 物料包装组
         * 使用 @PackagingGroup 代替
         */
//        PACKAGING("包装组"),
        /**
         * 直发现场
         */
        DIRECT_DISCOVERY("直发现场"),
        /**
         * 销售组织
         */
        SALES_ORG("销售组织"),
        /**
         * 销售渠道
         */
        DISTRIBUTION_CHANNEL("销售渠道"),
        /**
         * 商品组
         */
        DIVISION("商品组"),
        /**
         * 销售组
         */
        SALES_GROUP("销售组"),
        /**
         * 销售地域
         */
        SALES_DISTRICT("销售地域"),
        /**
         * 客户定价组
         */
        CUSTOMER_PRICE_GROUP("客户定价组"),
        /**
         * 物料定价组
         */
        MATERIAL_PRICE_GROUP("物料定价组"),
        /**
         * 销售场景分组（物料）
         */
        SO_MATERIAL_CATEGORY_GROUP("销售场景分组（物料）"),
        /**
         * 销售场景分组（客户Inforecord）
         */
        SO_INFORECORD_CATEGORY_GROUP("销售场景分组（客户Inforecord）"),
        /**
         * 装运要求（客户）
         */
        SO_SHIPPING_CONDITION("装运要求（客户）"),
        /**
         * 装运组（物料）
         */
        SO_LOADING_GROUP("装运组（物料）"),
        ;

        private final String label;

        @Override
        public String getCode() {
            return name();
        }

        public static CategoryEnum valueOfCode(String code) {
            return valueOf(code);
        }
    }
}