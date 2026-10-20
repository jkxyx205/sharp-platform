package com.rick.site.module.demo.dao;

import com.rick.db.repository.EntityCodeDAOImpl;
import com.rick.site.module.demo.entity.MyEnum;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Rick.Xu
 * @date 2025/11/19 14:24
 */
@Repository
public class MyEnumDAO extends EntityCodeDAOImpl<MyEnum, Long> {

    /**
     * @NotBlank 可以生效，基础父类 @Validated
     * @param name
     * @return
     */
    public List<MyEnum> findByName(@NotBlank String name){
        return select("name = ?", name);
    }
}
