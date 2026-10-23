package com.rick.site.module.dog.entity;

import com.rick.common.component.starter.model.ComponentBaseEntity;
import com.rick.db.repository.Column;
import com.rick.db.repository.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * @author Rick.Xu
 * @date 2026/10/23 11:38
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "t_dog", comment = "狗")
public class Dog extends ComponentBaseEntity<Long> {

    @Column(comment = "名字")
    @NotBlank
    String name;

    @Column(comment = "年龄")
    @NonNull
    Short age;

    @Column(columnDefinition = "text", comment = "描述")
    String description;
}