package com.rick.site.module.demo.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rick.common.component.starter.model.ComponentBaseCodeDescriptionEntity;
import com.rick.common.http.json.deserializer.EntityWithCodePropertyDeserializer;
import com.rick.db.repository.Embedded;
import com.rick.db.repository.Table;
import com.rick.meta.dict.model.DictType;
import com.rick.meta.dict.model.DictValue;
import com.rick.site.module.demo.model.CodeEnum;
import com.rick.site.module.demo.model.CodeIntegerEnum;
import com.rick.site.module.demo.model.NameEnum;
import com.rick.site.module.demo.model.SimpleEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "t_my_enum", comment = "测试枚举属性")
public class MyEnum extends ComponentBaseCodeDescriptionEntity<Long> {

    @NotBlank
    String name;

    NameEnum nameEnum;

    @JsonAlias("code_enum")
    CodeEnum codeEnum;

    @JsonAlias("simple_enum")
    SimpleEnum simpleEnum;

    CodeIntegerEnum codeIntegerEnum;

//    @Valid // this.validatorHelper.validateProperty(entity, propertyName); 需要
    @Embedded(columnPrefix = "material_type_", comment = "物料类型")
    @JsonAlias("materialType")
    @JsonDeserialize(using = EntityWithCodePropertyDeserializer.class)
    @DictType(type = "MATERIAL_TYPE")
    DictValue materialType;

//    @Valid
    @Embedded(columnPrefix = "judge_level_", comment = "等级")
    @DictType(type = "JudgeLevelEnum")
    @JsonDeserialize(using = EntityWithCodePropertyDeserializer.class)
    DictValue judgeLevel;

    /**
     * DictType，仅通过 sql 属性获取值。前端无法跟 type 一样通过 DictApi，获取字典数据。
     */
//    @Valid
    @Embedded(columnPrefix = "material_group_", comment = "物料组")
    @DictType(sql = "SELECT id, code, description label from sys_code_description WHERE code = ? AND category = 'MATERIAL'")
    @JsonDeserialize(using = EntityWithCodePropertyDeserializer.class)
    DictValue materialGroup;

}
