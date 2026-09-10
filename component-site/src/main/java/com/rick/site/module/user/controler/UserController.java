package com.rick.site.module.user.controler;

import com.rick.common.component.starter.controller.BaseApi;
import com.rick.site.module.user.entity.User;
import com.rick.site.module.user.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 CRUD。
 * 继承 BaseApi 即获得标准端点（实际路径均带 /{groupId} 前缀，经网关为 /api/site/{groupId}/users/...）：
 * GET    ""        分页列表（Grid，可按 type 等条件过滤）
 * GET    "detail"  分页列表（强类型实体）
 * GET    "one"     按条件查单条
 * GET    "new"     空白实体
 * GET    "{id}"    按 id 查
 * POST   ""        新增/更新（insertOrUpdate）
 * PUT    "{id}"    全量更新
 * PATCH  "{id}"    部分更新
 * DELETE "{id}"    删除
 */
@RestController
@RequestMapping("users")
public class UserController extends BaseApi<UserService, User, Long> {

    public UserController(UserService baseService) {
        super(baseService);
    }

}
