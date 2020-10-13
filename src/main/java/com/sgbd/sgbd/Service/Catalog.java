package com.sgbd.sgbd.Service;

import com.sgbd.sgbd.Model.Column;

public interface Catalog {

    void saveDatabase(String dbName);
    void dropDatabase(String dbName);
    void saveTable(String dbName, String tableName, String fileName, String rowLength, Column... columns);
    void dropTable(String dbName, String tableName);
}
