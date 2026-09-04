package com.rick.common.component.starter.config;

import com.rick.db.repository.TableDAO;
import com.rick.db.repository.model.EntityId;
import com.rick.db.repository.support.InsertUpdateCallback;
import com.rick.db.repository.support.baseinfo.ExtendInsertUpdateCallback;
import com.rick.db.repository.support.baseinfo.ExtendTableDAOImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public TableDAO tableDAO(@Autowired(required = false) NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new ExtendTableDAOImpl(namedParameterJdbcTemplate) {
            @Override
            public long getUserId() {
//                User user = UserContextHolder.get();
//                user = (user == null) ? User.builder().id(1L).build() : user;
//                return user.getId();
                return 1L;
            }

            @Override
            protected void addInsertInfo(Map<String, Object> paramMap) {
                paramMap.put("groupId", 100L);
            }
        };
    }

    @Bean
    public InsertUpdateCallback insertCallback() {
        return new ExtendInsertUpdateCallback() {
            @Override
            public void handler(boolean insert, EntityId<Long> entity, Map<String, Object> args) {
                super.handler(insert, entity, args);
//                baseEntityInfoGetter.setGroupId(100L)
            }
        };
    }
}
