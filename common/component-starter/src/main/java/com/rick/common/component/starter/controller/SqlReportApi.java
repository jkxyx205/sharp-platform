package com.rick.common.component.starter.controller;

import com.rick.common.component.starter.exception.ResourceNotFoundException;
import com.rick.common.http.HttpServletRequestUtils;
import com.rick.db.plugin.page.Grid;
import com.rick.db.plugin.page.GridUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * sql报表
 * @author Rick.Xu
 * @date 2023/6/14 11:13
 */
@RestController
@RequestMapping("api")
public class SqlReportApi {

    private static final Map<String, String> sqlMapping = new HashMap<>();

    static {
        // 必须把 id 查询出来
        sqlMapping.put("mm_material", "SELECT id, code, description FROM mm_material WHERE id = :id AND code = :code AND description LIKE :description");
        sqlMapping.put("masters", "SELECT hello AS \"hello\", remark AS \"remark\", description AS \"description\", create_by AS \"baseEntityInfo.createBy\", create_time AS \"baseEntityInfo.createTime\", update_by AS \"baseEntityInfo.updateBy\", update_time AS \"baseEntityInfo.updateTime\", is_deleted AS \"baseEntityInfo.deleted\", code AS \"code\", id AS \"id\" FROM demo_master WHERE hello = :hello AND remark = :remark AND description = :description AND create_by = :baseEntityInfo.createBy AND create_time = :baseEntityInfo.createTime AND update_by = :baseEntityInfo.updateBy AND update_time = :baseEntityInfo.updateTime AND is_deleted = :baseEntityInfo.deleted AND code = :code AND id = :id");
    }

    @GetMapping("sql/{key}")
    public Grid<Map<String, Object>> list(@PathVariable String key, HttpServletRequest request) {
        String sql = sqlMapping.get(key);
        if (Objects.isNull(sql)) {
            throw new ResourceNotFoundException(key);
        }

        return GridUtils.list(sql, HttpServletRequestUtils.getParameterMap(request));
    }

}
