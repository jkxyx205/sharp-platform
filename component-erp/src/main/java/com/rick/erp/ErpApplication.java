package com.rick.erp;

import com.rick.common.component.starter.config.ComponentConfig;
import com.rick.fileupload.client.controller.DocumentController;
import com.rick.fileupload.client.support.DocumentDAO;
import com.rick.fileupload.client.support.DocumentServiceImpl;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@DependsOn("entityDAOSupport")
@Import({DocumentDAO.class, DocumentServiceImpl.class, DocumentController.class, ComponentConfig.class})
@EnableDubbo
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }

}
