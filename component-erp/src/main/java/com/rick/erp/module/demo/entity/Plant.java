package com.rick.erp.module.demo.entity;

import com.rick.common.component.starter.model.ComponentBaseCodeDescriptionEntity;
import com.rick.db.repository.Column;
import com.rick.db.repository.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * @author Rick.Xu
 * @date 2025/11/19 14:20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "mm_plant", comment = "库房")
public class Plant extends ComponentBaseCodeDescriptionEntity<Long> {

    @NotBlank
    String code;

    /**
     * [省id,市id，区/县id，街道id]
     */
    @Column(columnDefinition = "json")
    List<Long> areaPath;

    String provincePath;

    @Length(max = 128, message = "详细地址不能超过128个字符")
    String detailAddress;

    @Column(comment = "联系人")
    String contactPerson;

    @Column(comment = "联系方式")
    String contactNumber;

}
