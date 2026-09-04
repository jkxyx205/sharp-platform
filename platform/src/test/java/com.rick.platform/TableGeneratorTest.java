package com.rick.platform;

import com.rick.db.plugin.generator.TableGenerator;
import com.rick.platform.module.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Rick.Xu
 * @date 2025/11/10 16:27
 */
@SpringBootTest
public class TableGeneratorTest {

    @Autowired
    private TableGenerator tableGenerator;

    @Test
    public void testGeneratorTable() {
        tableGenerator.createTable(User.class);
    }
}

