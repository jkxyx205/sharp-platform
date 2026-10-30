package com.rick.erp.module.purchasing.service;

import com.rick.erp.module.common.service.ErpHeaderEntityBaseCodeServiceImpl;
import com.rick.erp.module.purchasing.dao.PurchaseOrderDAO;
import com.rick.erp.module.purchasing.entity.PurchaseOrder;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseOrderService extends ErpHeaderEntityBaseCodeServiceImpl<PurchaseOrderDAO, PurchaseOrder> {

    public PurchaseOrderService(PurchaseOrderDAO purchaseOrderDAO) {
        super(purchaseOrderDAO);
    }

    @Override
    protected void codeGenerate(PurchaseOrder entity) {
        //TODO 号段方法，生成 code
        entity.setCode("mmtc");
    }

    @Override
    protected void beforeInsertOrUpdate(PurchaseOrder entity) {
        super.beforeInsertOrUpdate(entity);

        for (PurchaseOrder.Item item : entity.getItemList()) {
            item.setPartnerId(entity.getPartnerId());
            item.setPartnerCode(entity.getPartnerCode());

            for (PurchaseOrder.DeliverySchedule deliverySchedule : item.getDeliveryScheduleList()) {
                deliverySchedule.setItemCode(item.getCode());
            }
        }
    }
}