package com.rick.platform.module.user.dao;

import com.rick.db.repository.EntityDAOImpl;
import com.rick.platform.module.user.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserDAO extends EntityDAOImpl<User, Long> {

}
