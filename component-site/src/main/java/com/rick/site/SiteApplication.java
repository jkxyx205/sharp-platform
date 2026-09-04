package com.rick.site;

import com.rick.common.component.starter.config.ComponentConfig;
import com.rick.common.component.starter.config.DatabaseConfig;
import com.rick.common.component.starter.config.WebMvcRegistrationsConfig;
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
@Import({DocumentDAO.class, DocumentServiceImpl.class, DocumentController.class,
        ComponentConfig.class, WebMvcRegistrationsConfig.class, DatabaseConfig.class})
@EnableDubbo
public class SiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiteApplication.class, args);
    }

}
