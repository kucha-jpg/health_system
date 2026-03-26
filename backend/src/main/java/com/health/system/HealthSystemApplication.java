package com.health.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@MapperScan("com.health.system.mapper")
public class HealthSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthSystemApplication.class, args);
    }
}
