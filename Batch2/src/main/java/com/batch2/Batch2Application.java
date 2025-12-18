package com.batch2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Batch2Application {
    public static void main(String[] args) {
        System.out.println("Executing Batch 2 - Report Generation Job");
        
        try {
            Thread.sleep(3000);
            System.out.println("Batch 2 completed successfully");
        } catch (InterruptedException e) {
            System.err.println("Batch 2 interrupted: " + e.getMessage());
        }
        
        System.exit(0);
    }
}