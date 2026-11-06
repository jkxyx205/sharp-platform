package com.rick.erp.module.common.service;

import com.rick.db.repository.EntityCodeDAO;
import com.rick.erp.module.common.model.HeaderEntity;
import com.rick.erp.module.common.model.ItemEntity;
import com.rick.erp.module.common.model.StockItemEntity;
import com.rick.erp.module.inventory.entity.ItemSerialNumber;
import com.rick.erp.module.material.entity.SerialNumber;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * @author Rick.Xu
 * @date 2025/11/15 07:55
 */
@Validated
public class ErpHeaderEntityBaseCodeServiceImpl<D extends EntityCodeDAO<T, Long>, T extends HeaderEntity<? extends ItemEntity>> extends ErpBaseCodeServiceImpl<D, T, Long> {

    public ErpHeaderEntityBaseCodeServiceImpl(D baseDAO) {
        super(baseDAO);
    }

    @Override
    protected void beforeFieldsFill(T entity) {
        // 设置 headerCode
        for (ItemEntity itemEntity : entity.getItemList()) {
            itemEntity.setHeaderCode(entity.getCode());

            // 如果是序列号，则设置 Item Code / SerialNumber / Material Info
            if (itemEntity instanceof StockItemEntity stockItemEntity) {
                if (CollectionUtils.isNotEmpty(stockItemEntity.getStockLocation().getItemSerialNumberList())) {
                    for (ItemSerialNumber itemSerialNumber : stockItemEntity.getStockLocation().getItemSerialNumberList()) {
                        itemSerialNumber.setItemCode(stockItemEntity.getCode());
                        SerialNumber serialNumber = itemSerialNumber.getSerialNumber();
                        if (Objects.isNull(serialNumber)) {
                            serialNumber = SerialNumber.builder().code(itemSerialNumber.getSerialNumberCode()).build();
                            itemSerialNumber.setSerialNumber(serialNumber);
                        }
                        serialNumber.setMaterialId(stockItemEntity.getStockLocation().getMaterialId()); // 可能需要通过工具 fill id
                        serialNumber.setMaterialCode(stockItemEntity.getStockLocation().getMaterialCode());
                    }
                }
            }
        }

    }
}
