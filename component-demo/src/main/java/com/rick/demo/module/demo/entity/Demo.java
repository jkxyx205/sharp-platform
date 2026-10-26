package com.rick.demo.module.demo.entity;

import com.rick.common.component.starter.model.ComponentBaseEntity;
import com.rick.db.repository.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * @author Rick.Xu
 * @date 2026/10/26 16:17
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "t_demo", comment = "demo")
public class Demo extends ComponentBaseEntity<Long> {
    String name;
}