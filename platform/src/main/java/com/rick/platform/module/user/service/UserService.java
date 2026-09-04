package com.rick.platform.module.user.service;

import com.rick.db.plugin.BaseServiceImpl;
import com.rick.platform.module.user.dao.UserDAO;
import com.rick.platform.module.user.entity.User;
import com.rick.platform.module.user.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService extends BaseServiceImpl<UserDAO, User, Long> {

    /** 性别默认值：未知（1=男 2=女 0=未知） */
    private static final char SEX_UNKNOWN = '0';

    private final PasswordEncoder passwordEncoder;

    public UserService(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        super(userDAO);
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册：手机号唯一性校验 + 密码加密入库。
     */
    public User register(String nickname, String mobile, String rawPassword, Character sex) {
        if (exists("mobile = ?", mobile)) {
            throw new AuthException(HttpStatus.CONFLICT, "手机号已注册");
        }
        User user = User.builder()
                .nickname(nickname)
                .mobile(mobile)
                .password(passwordEncoder.encode(rawPassword))
                .sex(sex == null ? SEX_UNKNOWN : sex)
                .locked(false)
                .build();
        return insert(user); // insert 回填自增 id
    }

    /**
     * 登录：按手机号查找并校验密码。
     * 用户不存在与密码错误统一提示，避免账号枚举。
     */
    public User login(String mobile, String rawPassword) {
        List<User> users = select("mobile = ?", mobile);
        if (users.isEmpty() || !passwordEncoder.matches(rawPassword, users.get(0).getPassword())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "手机号或密码错误");
        }
        User user = users.get(0);
        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "账号已锁定，请联系管理员");
        }
        return user;
    }
}
