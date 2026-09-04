package com.rick.platform.module.user.entity;


import com.rick.db.repository.Column;
import com.rick.db.repository.Table;
import com.rick.db.repository.model.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * Created by rick on 7/13/18.
 * 用户基本信息
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(value = "sys_user", comment = "用户")
public class User extends BaseEntity<Long> {

    /**
     * 名字
     */
    @NotBlank(message = "名字不能为空")
    @NotNull
    String nickname;

    /**
     * 手机号码
     */
    @NotBlank
    @NotNull
    String mobile;

    /**
     * 加密后的密码（BCrypt 固定 60 字符，默认 varchar(32) 不够）
     */
    @NotBlank
    @NotNull
    @Column(columnDefinition = "varchar(100)")
    String password;

    /**
     * 头像
     */
    String avatar;

    /**
     * 性别
     */
    @NotNull
    Character sex;

    /**
     * 签名
     */
    @Size(max = 50, message = "签名最多50个字符")
    String signature;

    /**
     * 是否冻结
     */
    @NotNull
    @Column("is_locked")
    Boolean locked;

}