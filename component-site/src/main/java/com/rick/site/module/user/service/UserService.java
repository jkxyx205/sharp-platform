package com.rick.site.module.user.service;

import com.rick.db.plugin.BaseServiceImpl;
import com.rick.site.module.user.dao.UserDAO;
import com.rick.site.module.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserService extends BaseServiceImpl<UserDAO, User, Long> {

    public UserService(UserDAO userDAO) {
        super(userDAO);
    }

}
