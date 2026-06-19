package com.hotel.backend.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This runner prints the current database name and all tables present in that schema.
 * It helps verify that the Spring Boot application is connecting to the expected MySQL instance.
 */
@Component
public class DatabaseCheckRunner implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void run(String... args) throws Exception {
        // Print the database we are connected to
        String dbName = (String) entityManager.createNativeQuery("SELECT DATABASE()").getSingleResult();
        System.out.println("[DB CHECK] Connected to database: " + dbName);

        // Retrieve all table names in the current schema
        List<String> tables = entityManager.createNativeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = :schema"
        ).setParameter("schema", dbName).getResultList();

        System.out.println("[DB CHECK] Tables in schema '" + dbName + "':");
        tables.forEach(t -> System.out.println("   - " + t));
    }
}
