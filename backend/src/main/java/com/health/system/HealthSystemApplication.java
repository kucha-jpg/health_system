package com.health.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.health.system.mapper")
public class HealthSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthSystemApplication.class, args);
    }
}
