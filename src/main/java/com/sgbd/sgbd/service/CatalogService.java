package com.sgbd.sgbd.service;

import com.sgbd.sgbd.model.Column;

<<<<<<< HEAD:src/main/java/com/sgbd/sgbd/service/CatalogService.java
public interface CatalogService {
=======

import java.util.List;

public interface Catalog {
>>>>>>> a32ac8ea6f33068591847c83717a983248d98070:src/main/java/com/sgbd/sgbd/Service/Catalog.java

    void saveDatabase(String dbName);
    void dropDatabase(String dbName);
    void saveTable(String dbName, String tableName, String fileName, String rowLength, Column... columns);
    void dropTable(String dbName, String tableName);
    List<String> getAllDatabase();
}
