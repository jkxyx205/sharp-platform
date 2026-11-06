package com.rick.erp.module.purchasing.service;

import com.rick.common.http.exception.BizException;
import com.rick.common.http.model.ResultUtils;
import com.rick.erp.module.common.service.ErpHeaderEntityBaseCodeServiceImpl;
import com.rick.erp.module.purchasing.dao.PurchaseOrderDAO;
import com.rick.erp.module.purchasing.entity.PurchaseOrder;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PurchaseOrderService extends ErpHeaderEntityBaseCodeServiceImpl<PurchaseOrderDAO, PurchaseOrder> {

    public PurchaseOrderService(PurchaseOrderDAO purchaseOrderDAO) {
        super(purchaseOrderDAO);
    }

    @Override
    protected void codeGenerate(PurchaseOrder purchaseOrder) {
        //TODO 号段方法，生成 code
        purchaseOrder.setCode("mmtc");
    }

    @Override
    protected void validate(PurchaseOrder purchaseOrder) {
        Map<String, Object> errorsMap = new LinkedHashMap<>(16);
        // TODO 设计 messages 结构，消息类似一张表： 序号 消息文本 类型（ERROR/WARNING）代替 errorsMap
        if (MapUtils.isNotEmpty(errorsMap)) {
            throw new BizException(ResultUtils.fail(50001,"XXX 验证失败", errorsMap));
        }
    }

    @Override
    protected void beforeInsertOrUpdate(PurchaseOrder purchaseOrder) {
        super.beforeInsertOrUpdate(purchaseOrder);

        for (PurchaseOrder.Item item : purchaseOrder.getItemList()) {
            item.setPartnerId(purchaseOrder.getPartnerId());
            item.setPartnerCode(purchaseOrder.getPartnerCode());

            for (PurchaseOrder.DeliverySchedule deliverySchedule : item.getDeliveryScheduleList()) {
                deliverySchedule.setItemCode(item.getCode());
            }
        }
    }
}