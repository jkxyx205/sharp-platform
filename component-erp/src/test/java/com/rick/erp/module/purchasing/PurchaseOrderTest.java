package com.rick.erp.module.purchasing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.rick.common.util.JsonUtils;
import com.rick.erp.BaseTest;
import com.rick.erp.module.common.model.CurrencyEnum;
import com.rick.erp.module.common.model.SpecialStockEnum;
import com.rick.erp.module.common.model.StockLocation;
import com.rick.erp.module.common.model.StockTypeEnum;
import com.rick.erp.module.purchasing.entity.PurchaseOrder;
import com.rick.erp.module.purchasing.model.ConfirmationControlEnum;
import com.rick.erp.module.purchasing.model.ItemCategoryEnum;
import com.rick.erp.module.purchasing.service.PurchaseOrderService;
import com.rick.meta.dict.model.DictValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

public class PurchaseOrderTest extends BaseTest<PurchaseOrderService, PurchaseOrder, Long> {

    public PurchaseOrderTest(@Autowired PurchaseOrderService baseService) {
        super(baseService);
    }

    @Test
    public void test001_SaveOrUpdate() {
        super.testInsertOrUpdate(PurchaseOrder.builder()
//                .code("mmtc") // 修改提供 code
                .purchaseOrderTypeCode("0001")
                .partnerCode("0001")
                .partnerName("苏州云点信息系统工程有限公司")
                .documentDate(LocalDate.now())
                .purchasingOrg(new DictValue("0001"))
                .purchasingGroupCode("0001")
//                .partnerReferenceList(Arrays.asList(
//                                PartnerReference.builder().partnerCategory(PartnerReference.PartnerCategoryEnum.PO).partnerFunction(PartnerFunctionEnum.VN).partnerCode("0001").build(),
//                                PartnerReference.builder().partnerCategory(PartnerReference.PartnerCategoryEnum.PO).partnerFunction(PartnerFunctionEnum.RE).partnerCode("0001").build(),
//                                PartnerReference.builder().partnerCategory(PartnerReference.PartnerCategoryEnum.PO).partnerFunction(PartnerFunctionEnum.CR).partnerCode("0001").build(),
//                                PartnerReference.builder().partnerCategory(PartnerReference.PartnerCategoryEnum.PO).partnerFunction(PartnerFunctionEnum.OA).partnerCode("0001").build()
//                        )
//                )
                .description("短文本描述")
                .remark("长文本备注")
                .itemList(Arrays.asList(PurchaseOrder.Item.builder()
                        .code("10")// // 修改提供 header_code + item_code
                        .parentId(0L)
                        .parentCode("20")
                        .itemConfigCode("0001")
                        .itemCategory(ItemCategoryEnum.STANDARD)
                        .description("手机")
//                        .shortText("手机")
                        .quantity(BigDecimal.TEN.setScale(3))
                        .orderUnitCode("KG")
                        .baseUnitQuantity(BigDecimal.TEN.setScale(3))
                        .baseUnitCode("KG")
                        .stockLocation(StockLocation.builder()
                                .materialCode("0001")
//                                .serialNumberCode("0001")
                                .batchCode("0001")
                                .plantCode("0001")
                                .storageLocationCode("0001")
                                .stockType(StockTypeEnum.UNRESTRICTED)
                                .specialStock(SpecialStockEnum.SUPPLIER_CONSIGNMENT)
                                .specialStockValue("00011")
//                                .itemSerialNumberList()
                                .build())
                        .netWeight(BigDecimal.valueOf(10).setScale(3))
                        .grossWeight(BigDecimal.valueOf(12).setScale(3))
                        .weightUnitCode("KG")
                        .volume(BigDecimal.valueOf(20).setScale(3))
                        .volumeUnitCode("M3")
                        .netPrice(BigDecimal.valueOf(99.9).setScale(3))
                        .currency(CurrencyEnum.CNY)
                        .deliveryDate(LocalDate.now())
                        .per(1)
                        .priceUnitCode("G")
                        .materialGroup(new DictValue("0001"))
                        .infoRecordCode("0001")
                        .overDeliveryTolerance(BigDecimal.valueOf(11.2).setScale(3))
                        .underDeliveryTolerance(BigDecimal.valueOf(10).setScale(3))
                        .unlimited(false)
                        .goodsReceipt(true)
                        .goodsReceiptNonValuated(true)
                        .deliveryComplete(false)
                        .invoiceReceipt(true)
                        .goodsReceiptBasedInvoice(true)
                        .finalInvoice(false)
                        .free(false)
                        .returns(false)
                        .directDiscovery(false)
                        .directDiscoveryGroup(new DictValue("0001"))
                        .confirmationControl(ConfirmationControlEnum.NONE)
                        .deliveryScheduleList(Arrays.asList(
                                PurchaseOrder.DeliverySchedule.builder()
//                                        .id(1146900935151194112L) 修改提供 id
                                        .deliveryDate(LocalDate.now().plusDays(3))
                                        .scheduledQuantity(BigDecimal.valueOf(10).setScale(3))
                                        .build(),
                                PurchaseOrder.DeliverySchedule.builder()
                                        .deliveryDate(LocalDate.now().plusDays(5))
                                        .scheduledQuantity(BigDecimal.valueOf(15).setScale(3))
                                        .build()
                        ))
//                        .requisition(ItemReference.builder().code("1000000008").itemCode("10").build())
                        .remark("备注")
                        .build()))
                .build());
    }

    @Test
    public void testBySelect() {
        PurchaseOrder purchaseOrder = baseService.selectByCode("mmtc").get();
        System.out.println(purchaseOrder);
        System.out.println(JsonUtils.toJson(purchaseOrder));

        // 通过属性值操作
//        Map<String, Object> stringMap = (Map<String, Object>) JsonUtils.objectToMap(purchaseOrder);
//        for (Object item : (ArrayList) stringMap.get("itemList")) {
//            Map<String, Object> itemMap = ( Map<String, Object>)item;
//            itemMap.put("materialCode", "new");
//        }
//
//        PurchaseOrder purchaseOrder1 = JsonUtils.toObject(JsonUtils.toJson(stringMap), PurchaseOrder.class);

        JsonNode jsonNode = JsonUtils.toJsonNode(purchaseOrder.getItemList());
        if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (JsonNode node : arrayNode) {
//                String materialCode = node.get("materialCode").asText();
                TextNode materialCode = (TextNode) node.get("materialCode");

                ObjectNode objectNode = (ObjectNode) node;
                objectNode.put("materialCode", "new0001");

                PurchaseOrder.Item purchaseItem = JsonUtils.toObject(node, PurchaseOrder.Item.class);
                System.out.println("====");

            }

        }

//        PurchaseOrder purchaseOrder1 = JsonUtils.toObject(jsonNode, PurchaseOrder.class);
    }
}