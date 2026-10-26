package com.rick.demo.module.demo.dao;

import com.rick.db.repository.EntityDAOImpl;
import com.rick.demo.module.demo.entity.Demo;
import org.springframework.stereotype.Repository;

/**
 * @author Rick.Xu
 * @date 2026-10-26 16:23:18
 */
@Repository
public class DemoDAO extends EntityDAOImpl<Demo, Long> {

}