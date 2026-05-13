package com.competition;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CompetitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompetitionApplication.class, args);
    }

    @Bean
    CommandLineRunner startupMessage() {
        return args -> {
            System.out.println();
            System.out.println("========================================");
            System.out.println("  竞赛科研管理系统 - 后端可正常运行");
            System.out.println("  http://localhost:8080");
            System.out.println("========================================");
            System.out.println();
        };
    }
}
