package com.sgbd.sgbd.service;

import com.sgbd.sgbd.model.Column;

public interface CatalogService {

    void saveDatabase(String dbName);
    void dropDatabase(String dbName);
    void saveTable(String dbName, String tableName, String fileName, String rowLength, Column... columns);
    void dropTable(String dbName, String tableName);
}
