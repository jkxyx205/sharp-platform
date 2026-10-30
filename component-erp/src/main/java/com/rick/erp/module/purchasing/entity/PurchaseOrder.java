package com.rick.erp.module.purchasing.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rick.common.component.starter.model.ComponentBaseEntity;
import com.rick.common.http.json.deserializer.EntityWithCodePropertyDeserializer;
import com.rick.db.repository.*;
import com.rick.db.repository.support.CodeFillUncheck;
import com.rick.db.repository.support.category.RowCategory;
import com.rick.erp.module.common.constant.DictValueSQLConstants;
import com.rick.erp.module.common.constant.HeaderConstants;
import com.rick.erp.module.common.model.BoundTypeEnum;
import com.rick.erp.module.common.model.CurrencyEnum;
import com.rick.erp.module.common.model.HeaderEntity;
import com.rick.erp.module.common.model.StockItemEntity;
import com.rick.erp.module.purchasing.model.ConfirmationControlEnum;
import com.rick.erp.module.purchasing.model.ItemCategoryEnum;
import com.rick.meta.dict.model.DictType;
import com.rick.meta.dict.model.DictValue;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Rick.Xu
 * @date 2023/3/31 15:32
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "purchasing_order", comment = "采购订单", referenceColumnId = HeaderConstants.HEADER_ID_COLUMN_NAME)
public class PurchaseOrder extends HeaderEntity<PurchaseOrder.Item> {

    @Column(comment = "订单类型")
    Long purchaseOrderTypeId;

    @NotNull
    String purchaseOrderTypeCode;

//    @Embedded(columnPrefix="partner_")
//    @JsonAlias("partnerCode")
//    @JsonDeserialize(using = NamePropertyDeserializer.class)
//    @Select(value = DictValueSQLConstants.PARTNER_DICT, params="id@partner.id")
//    private IdCodeValue partner;

    @Column(comment = "供应商id")
    Long partnerId;

    @NotNull
    String partnerCode;

    @Column(comment = "一次供应商填写供应商名称")
    String partnerName;

    @Column(comment = "凭证日期")
    LocalDate documentDate;

    @Embedded(columnPrefix="purchasing_org_", comment = "采购组织")
    @JsonAlias("purchasingOrgCode")
    @JsonDeserialize(using = EntityWithCodePropertyDeserializer.class)
    @DictType(sql = DictValueSQLConstants.PURCHASING_ORG_DICT)
    private DictValue purchasingOrg;

//    @ManyToOne(value = "purchasing_group_id", comment = "采购组")
//    private PurchasingGroup purchasingGroup;

    private String purchasingGroupCode;

//    @OneToMany(joinColumnId = "reference_id", mappedBy = "referenceId")
//    private List<PartnerReference> partnerReferenceList;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @SuperBuilder
    @Table(value = "purchasing_order_item", comment = "采购订单行项目")
    @CodeFillUncheck
    public static class Item extends StockItemEntity {

        @Column(comment = "上级母行")
        Long parentId;

        @Column(comment = "上级母行")
        String parentCode;

        @Column(comment = "供应商id")
        Long partnerId;

        String partnerCode;

        @Column(comment = "行项目分类")
        Long itemConfigId;

        @NotNull
        String itemConfigCode;

        @NotNull
        @Column(comment = "采购场景")
        ItemCategoryEnum itemCategory;

//        @Embedded(comment = "库存八君子")
//        @JsonUnwrapped
//        StockLocation stockLocation;

//        @Column(comment = "短文本")
//        String shortText;

        @Column(comment = "订单数量")
        BigDecimal quantity;

        @Column(comment = "订单单位")
        @DictType(sql = DictValueSQLConstants.UNIT_DICT)
        String orderUnitCode;

        @Column(comment = "基本单位数量")
        BigDecimal baseUnitQuantity;

        @Column(comment = "基本单位")
        @DictType(sql = DictValueSQLConstants.UNIT_DICT)
        String baseUnitCode;

        @Column(comment = "净重")
        BigDecimal netWeight;

        @Column(comment = "毛重")
        BigDecimal grossWeight;

        @Column(comment = "重量单位")
        @DictType(type = "MASS", sql = DictValueSQLConstants.UNIT_MASS_DICT)
        String weightUnitCode;

        @Column(comment = "体积")
        @DictType(sql = DictValueSQLConstants.UNIT_VOLUME_DICT)
        BigDecimal volume;

        @Column(comment = "体积单位")
        String volumeUnitCode;

        @Column(comment = "交货日期")
        LocalDate deliveryDate;

        @Column(comment = "净价")
        BigDecimal netPrice;

        CurrencyEnum currency;

        @Column(comment = "计价数量")
        Integer per;

        @Column(comment = "计价单位")
        String priceUnitCode;

        @NotNull
        @Embedded(columnPrefix="material_group_", comment = "物料组")
        @JsonAlias("materialGroupCode")
        @JsonDeserialize(using = EntityWithCodePropertyDeserializer.class)
        @DictType(sql = DictValueSQLConstants.MATERIAL_GROUP_DICT)
//        @DictType(type = "MATERIAL_GROUP_DICT")
        DictValue materialGroup;

        Long infoRecordId;

        String infoRecordCode;

        @Column(comment = "过度交货限度")
        BigDecimal overDeliveryTolerance;

        @Column(comment = "交货不足限度")
        BigDecimal underDeliveryTolerance;

        @Column(value = "is_unlimited", comment = "无限制")
        Boolean unlimited;

        @Column(value = "is_goods_receipt", comment = "收货")
        Boolean goodsReceipt;

        @Column(value = "is_goods_receipt_non_valuated", comment = "不计价收货")
        Boolean goodsReceiptNonValuated;

        @Column(value = "is_delivery_complete", comment = "收货已完成")
        Boolean deliveryComplete;

        @Column(value = "is_invoice_receipt", comment = "收票")
        Boolean invoiceReceipt;

        @Column(value = "is_gr_based_invoice", comment = "基于收货收票")
        Boolean goodsReceiptBasedInvoice;

        @Column(value = "is_final_invoice", comment = "最终收票")
        Boolean finalInvoice;

        @Column(value = "is_free", comment = "免费")
        Boolean free;

        @Column(value = "is_return", comment = "退货")
        Boolean returns;

        @Column(value = "is_direct_discovery", comment = "直发现场")
        Boolean directDiscovery;

        @Embedded(columnPrefix="direct_discovery_group_", comment = "直发理由")
        @JsonAlias("directDiscoveryGroupCode")
        @JsonDeserialize(using = EntityWithCodePropertyDeserializer.class)
        @DictType(sql = DictValueSQLConstants.DIRECT_DISCOVERY_GROUP_DICT)
        DictValue directDiscoveryGroup;

        @Column(comment = "确认控制")
        ConfirmationControlEnum confirmationControl;

        /**
         * 交货计划
         */
        @OneToMany(joinColumnId = "item_id", mappedBy = "itemId")
        List<DeliverySchedule> deliveryScheduleList;

//        /**
//         * 采购申请引用
//         */
//        @Embedded(columnPrefix = "requisition_", comment = "采购申请引用")
//        @JsonUnwrapped(prefix = "requisition")
//        @ItemReferenceEntity(mainEntity = PurchaseRequisition.class, itemEntity = PurchaseRequisition.Item.class)
//        ItemReference requisition;

        @Override
        public BoundTypeEnum getBoundType() {
            return returns ? BoundTypeEnum.OUTBOUND : BoundTypeEnum.INBOUND;
        }
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @SuperBuilder
    @Table(value = "purchasing_order_item_delivery_schedule", comment = "交货计划")
    public static class DeliverySchedule extends ComponentBaseEntity<Long> implements RowCategory<Long> {

        @Column(nullable = false)
        Long itemId;

        String itemCode;

        LocalDate deliveryDate;

        BigDecimal scheduledQuantity;

        @Override
        public Long getCategory() {
            return itemId;
        }

        @Override
        public void setCategory(Long itemId) {
            setItemId(itemId);
        }

        @Transient
        BigDecimal goodsReceiptQuantity;

        public BigDecimal getOpenQuantity() {
            return scheduledQuantity.subtract(ObjectUtils.defaultIfNull(goodsReceiptQuantity, BigDecimal.ZERO));
        }
    }


    }