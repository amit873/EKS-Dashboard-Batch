package com.batch1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Batch1Application {
    public static void main(String[] args) {
        System.out.println("Executing Batch 1 - Data Processing Job");
        
        try {
            Thread.sleep(5000);
            System.out.println("Batch 1 completed successfully");
        } catch (InterruptedException e) {
            System.err.println("Batch 1 interrupted: " + e.getMessage());
        }
        
        System.exit(0);
    }
}