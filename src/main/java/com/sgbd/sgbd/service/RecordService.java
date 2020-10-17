package com.sgbd.sgbd.service;

import com.sgbd.sgbd.model.Record;

import java.util.Map;


public interface RecordService {


    void saveRecord(String dbName, String tableName, Record record);

    void deleteRecord(String dbName, String tableName, Record record);

    void updateRecord(String dbName, String tableName, Record record);

    Map findAll(String dbName, String tableName);
}
