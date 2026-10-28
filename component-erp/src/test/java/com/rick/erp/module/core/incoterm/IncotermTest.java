package com.rick.erp.module.core.incoterm;

import com.rick.db.repository.support.EntityCodeIdFillService;
import com.rick.erp.BaseTest;
import com.rick.erp.module.core.incoterm.entity.Incoterm;
import com.rick.erp.module.core.incoterm.service.IncotermService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class IncotermTest extends BaseTest<IncotermService, Incoterm, Long>  {

    EntityCodeIdFillService codeIdFillService;

    public IncotermTest(@Autowired IncotermService baseService, @Autowired EntityCodeIdFillService codeIdFillService) {
        super(baseService);
        this.codeIdFillService = codeIdFillService;
    }

    @Test
    protected void testInsert() {
      super.testInsert(Incoterm.builder()
                      .code("ZWORK")
                      .description("ZWORK desc")
                      .locationRequired(true)
              .build());
    }

    @Test
    public void testSelectByCode() {
        Optional<Incoterm> optional = baseService.selectByCode("FOB");
        assertEquals(true, optional.isPresent());
        Incoterm incoterm = optional.get();
        assertEquals("Free on board", incoterm.getDescription());
        log.info(incoterm.getDescription());
    }

    @Test
    public void testExists() {
        assertEquals(true, baseService.exists("code = ?", "FOB"));
    }

    @Test
    public void testExists2() {
        assertEquals(true, baseService.exists("code = :code", Map.of("code", "FOB")));
    }

    @Test
    public void testFill() {
        Incoterm zwork = Incoterm.builder()
                .code("ZWORK")
                .build();
        codeIdFillService.fill(zwork);
        assertNotNull(zwork.getId());
    }

    @Test
    public void testSelectAll() {
        List<Incoterm> incotermList = baseService.selectAll();
        System.out.println(incotermList.size());
    }

    @Test
    public void testSave() {
        // 清空自定义
//        baseService.saveOrUpdate(null);

        // 编辑 + 删除
        Incoterm zwork = Incoterm.builder()
                .code("ZWORK")
                .description("Edit 编辑222")
                .locationRequired(true)
                .build();

        Incoterm job = Incoterm.builder()
                .code("ZJOB2")
                .description("Insert 插入222")
                .locationRequired(true)
                .build();

//        codeIdFillService.fill(zwork);
        baseService.saveOrUpdate(List.of(zwork, job));

    }
}
