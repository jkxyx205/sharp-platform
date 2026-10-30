package com.rick.erp.module.common.model;

import com.rick.common.component.starter.model.ComponentBaseCodeDescriptionEntity;
import com.rick.db.repository.Column;
import com.rick.db.repository.support.CodeFillIgnore;
import com.rick.db.repository.support.category.RowCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.Length;

import static com.rick.db.repository.support.Constants.CODE_COLUMN_NAME;

/**
 * @author Rick.Xu
 * @date 2026/5/4 08:13
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
public class ItemEntity extends ComponentBaseCodeDescriptionEntity<Long> implements RowCategory<String> {

    @Length(max = 4, message = "编号不能超过4个字符")
    @NotBlank
    @Pattern(regexp = "^\\d+$", message = "CODE只能包含数字")
    @Column(value = CODE_COLUMN_NAME, comment = "外部可见，唯一code", nullable = false)
    String code;

    @Column(updatable = false, comment = "foreign key")
    Long headerId;

    @Column(updatable = false, comment = "foreign key")
    @CodeFillIgnore
    String headerCode;

    @Column(columnDefinition = "varchar(1024)", comment = "备注")
    String remark;

    @Override
    public String getCategory() {
        return headerCode;
    }

    @Override
    public void setCategory(String headerCode) {
        this.headerCode = headerCode;
    }

}