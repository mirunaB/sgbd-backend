package com.sgbd.sgbd.service;

import com.sgbd.sgbd.model.Column;
import com.sgbd.sgbd.model.Index;

import java.util.List;
import java.util.Map;

public interface CatalogService {

    void saveDatabase(String dbName);
    void dropDatabase(String dbName);
    void saveTable(String dbName, String tableName, String fileName, String rowLength, Column... columns);
    void dropTable(String dbName, String tableName);
    void addIndex(String dbName, String tableName, Index index);
    List<String> getAllDatabase();
    List<String> getAllColumnNameForTable(String dbName,String tableName);
    List<Column> getAllColumnForTable(String dbName,String tableName);
    List<String> getAllTablesForDb(String dbName);
    Map<String,String> getAnotherTablesForDb(String dbName, String tableName);

}
