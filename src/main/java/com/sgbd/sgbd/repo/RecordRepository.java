package com.sgbd.sgbd.repo;

import com.sgbd.sgbd.model.Record;

import java.util.Map;
import java.util.Set;

public interface RecordRepository {

    void save(String dbName, String tableName, Record record);
    Map findAll(String dbName, String tableName);
    Record findById(String dbName, String tableName, Set<String> id);
    void update(String dbName, String tableName, Record record);
    void delete(String dbName, String tableName, Record record);
}
