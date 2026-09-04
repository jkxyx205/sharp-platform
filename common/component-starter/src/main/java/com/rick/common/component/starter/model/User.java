package com.rick.common.component.starter.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    Long id;

    String mobile;

    Long groupId;
}
