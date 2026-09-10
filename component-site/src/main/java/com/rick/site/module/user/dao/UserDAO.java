package com.rick.site.module.user.dao;

import com.rick.db.repository.support.category.CategoryEnumEntityDAOImpl;
import com.rick.site.module.user.entity.User;
import com.rick.site.module.user.entity.UserType;
import org.springframework.stereotype.Repository;

/**
 * 分类字段是枚举 → CategoryEnumEntityDAOImpl，DB 按枚举 code 存取
 * （与建表 CHECK IN、实体写入、valueOfCode 读取全链一致；UserType 的 getCode() 返回 name()）。
 * 分类列名为 type：无参构造默认 "category"，必须显式指定。
 */
@Repository
public class UserDAO extends CategoryEnumEntityDAOImpl<User, Long, UserType> {

    public UserDAO() {
        super("type");
    }
}
