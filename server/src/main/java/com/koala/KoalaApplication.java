package com.koala;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.koala.mapper")
public class KoalaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KoalaApplication.class, args);
    }
}
