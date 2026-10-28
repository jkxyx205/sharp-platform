package com.rick.erp.module.core.group;

import com.rick.db.repository.support.EntityCodeIdFillService;
import com.rick.erp.BaseTest;
import com.rick.erp.module.core.group.entity.CodeDescription;
import com.rick.erp.module.core.group.service.CodeDescriptionService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CodeDescriptionTest extends BaseTest<CodeDescriptionService, CodeDescription, Long>  {

    EntityCodeIdFillService codeIdFillService;

    public CodeDescriptionTest(@Autowired CodeDescriptionService baseService, @Autowired EntityCodeIdFillService codeIdFillService) {
        super(baseService);
        this.codeIdFillService = codeIdFillService;
    }

//    @Test
//    protected void testInsert() {
//      super.testInsert(Incoterm.builder()
//                      .code("ZWORK")
//                      .description("ZWORK desc")
//                      .locationRequired(true)
//              .build());
//    }
//
//    @Test
//    public void testSelectByCode() {
//        Optional<Incoterm> optional = baseService.selectByCode("FOB");
//        assertEquals(true, optional.isPresent());
//        Incoterm incoterm = optional.get();
//        assertEquals("Free on board", incoterm.getDescription());
//        log.info(incoterm.getDescription());
//    }
//
//    @Test
//    public void testExists() {
//        assertEquals(true, baseService.exists("code = ?", "FOB"));
//    }
//
//    @Test
//    public void testExists2() {
//        assertEquals(true, baseService.exists("code = :code", Map.of("code", "FOB")));
//    }
//
//    @Test
//    public void testFill() {
//        Incoterm zwork = Incoterm.builder()
//                .code("ZWORK")
//                .build();
//        codeIdFillService.fill(zwork);
//        assertNotNull(zwork.getId());
//    }

    @Test
    public void testSelectAll() {
        List<CodeDescription> codeDescriptionList = baseService.selectAll(CodeDescription.CategoryEnum.MATERIAL);

//        System.out.println(incotermList.size());
    }

    @Test
    public void testSave() {
        // 清空自定义
//        baseService.saveOrUpdate(CodeDescription.CategoryEnum.MATERIAL, null);

        // 编辑 + 删除
        CodeDescription zwork = CodeDescription.builder()
                .code("M_ZWORK")
                .description("M_ZWORK editxxooo")
                .sort(1)
                .build();

        CodeDescription job = CodeDescription.builder()
                .code("M_ZJOB2")
                .description("M_ZJOB2 INSERT")
                .sort(2)
                .build();

        baseService.insertOrUpdate(CodeDescription.CategoryEnum.MATERIAL, List.of(zwork, job));
    }
}
