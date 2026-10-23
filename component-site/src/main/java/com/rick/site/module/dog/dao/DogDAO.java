package com.rick.site.module.dog.dao;

import com.rick.db.repository.EntityDAOImpl;
import com.rick.site.module.dog.entity.Dog;
import org.springframework.stereotype.Repository;

/**
 * @author Rick.Xu
 * @date 2026-10-23 12:00:57
 */
@Repository
public class DogDAO extends EntityDAOImpl<Dog, Long> {

}