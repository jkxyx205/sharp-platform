package com.rick.erp.module.purchasing.dao;

import com.rick.db.repository.support.category.CategoryEntityDAOImpl;
import com.rick.erp.module.purchasing.entity.PurchaseOrder;
import org.springframework.stereotype.Repository;

/**
 * @author Rick.Xu
 * @date 2026/10/28 18:21
 */
@Repository
public class DeliveryScheduleDAO extends CategoryEntityDAOImpl<PurchaseOrder.DeliverySchedule, Long, Long> {

    public DeliveryScheduleDAO() {
        super("item_id");
    }
}
