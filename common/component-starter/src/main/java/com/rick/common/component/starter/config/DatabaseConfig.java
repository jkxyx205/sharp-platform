package com.rick.common.component.starter.config;

import com.rick.common.component.starter.model.GroupIdGetter;
import com.rick.common.component.starter.model.User;
import com.rick.common.component.starter.model.UserContextHolder;
import com.rick.db.repository.JdbcTemplateCallback;
import com.rick.db.repository.TableDAO;
import com.rick.db.repository.model.EntityId;
import com.rick.db.repository.support.InsertUpdateCallback;
import com.rick.db.repository.support.baseinfo.ExtendInsertUpdateCallback;
import com.rick.db.repository.support.baseinfo.ExtendTableDAOImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public TableDAO tableDAO(@Autowired(required = false) NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new ExtendTableDAOImpl(namedParameterJdbcTemplate) {
            @Override
            public long getUserId() {
                User user = UserContextHolder.get();
                return (user == null || user.getId() == null) ? 1L : user.getId();
            }

            public long getGroupId() {
                User user = UserContextHolder.get();
                long groupId =  (user == null || user.getGroupId() == null) ? 1L : user.getGroupId();
                return groupId;
            }

            @Override
            protected void addInsertInfo(Map<String, Object> paramMap) {
                User user = UserContextHolder.get();
                long groupId =  (user == null || user.getGroupId() == null) ? 1L : user.getGroupId();
                paramMap.put("group_id", groupId);
            }

            @Override
            public int update(String tableName, String columnsCondition, String condition, Map<String, Object> paramMap) {
                paramMap = new HashMap<>(paramMap);
                paramMap.put("group_id", getGroupId());
                return super.update(tableName, columnsCondition, condition + " AND group_id = :group_id", paramMap);
            }

            @Override
            public int update(String tableName, String columnsCondition, String condition, Object... args) {
                return super.update(tableName, columnsCondition, condition + " AND group_id = ?", ArrayUtils.addAll(args, getGroupId()));
            }

            public <E> List<E> select(Class<E> clazz, String sql, Object... args) {
                return super.select(clazz, this.addIsDeletedCondition(sql, () -> "AND group_id = ?"), ArrayUtils.addAll(args, getGroupId()));
            }

            public <E> List<E> select(String sql, Map<String, Object> paramMap, JdbcTemplateCallback<E> jdbcTemplateCallback) {
                paramMap = new HashMap<>(paramMap);
                paramMap.put("group_id", getGroupId());
                return super.select(this.addIsDeletedCondition(sql, () -> "AND group_id = :group_id"), paramMap, jdbcTemplateCallback);
            }

            public List<Map<String, Object>> select(String sql, Object... args) {
                return super.select(this.addIsDeletedCondition(sql, () -> "AND group_id = ?"), ArrayUtils.addAll(args, getGroupId()));
            }

        };
    }



    @Bean
    public InsertUpdateCallback insertCallback() {
        return new ExtendInsertUpdateCallback() {
            @Override
            public void handler(boolean insert, EntityId<Long> entity, Map<String, Object> args) {
                super.handler(insert, entity, args);

                if (entity instanceof GroupIdGetter groupIdGetter) {
                    groupIdGetter.setGroupId((Long) args.get("group_id"));
                }

            }
        };
    }
}
