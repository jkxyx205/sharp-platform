package com.rick.erp.module.common.model;

import com.rick.common.component.starter.model.ComponentBaseCodeDescriptionEntity;
import com.rick.db.repository.Column;
import com.rick.db.repository.OneToMany;
import com.rick.erp.module.common.constant.HeaderConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @author Rick.Xu
 * @date 2026/5/18 11:35
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
public class HeaderEntity<T extends ItemEntity> extends ComponentBaseCodeDescriptionEntity<Long> {

    @Column(columnDefinition = "varchar(1024)", comment = "备注")
    String remark;

    /**
     * @NotEmpty：等价于 @NotNull + 集合/字符串非空（size() > 0）。也就是说它已经保证了 size >= 1。
     * @Size(min = 1)：只校验 size >= 1，但如果值为 null，@Size 是直接跳过校验、视为合法的（Bean Validation 规范规定：大多数约束对 null 值默认放行，只有 @NotNull/@NotEmpty/@NotBlank 才会拦截 null
     */
    @Valid
    @NotEmpty
//    @Size(min = 1)

    @OneToMany(mappedBy = HeaderConstants.HEADER_ID_PROPERTY_NAME, joinColumnId = HeaderConstants.HEADER_ID_COLUMN_NAME)
    List<T> itemList;

    /**
     * 手写非泛型 builder()，让 Lombok（methodExists 检查）和 IDEA Lombok 插件（hasMethod 检查）都跳过自动生成。
     * 原因：泛型子类固化父类类型参数（extends ComponentBaseCodeDescriptionEntity&lt;Long&gt;）时，Lombok 生成的
     * {@code <T>builder()} 与继承的 {@code <ID>builder()} 擦除相同但互不构成子签名
     * （javac: name clash; IDEA: incompatible return type）；而非泛型 builder() 可按擦除子签名规则合法隐藏父类方法，
     * 与 ItemEntity 的生成形态一致。勿删，删除后编译复现该错误。
     */
    public static HeaderEntityBuilder<?, ?, ?> builder() {
        return new HeaderEntityBuilderImpl<>();
    }
}
