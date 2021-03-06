package com.sgbd.sgbd.service;

import com.sgbd.sgbd.model.Column;
import com.sgbd.sgbd.model.Index;
import com.sgbd.sgbd.model.JoinReq;
import com.sgbd.sgbd.model.Record;

import java.util.List;
import java.util.Map;


public interface RecordService {

    void saveRecord(String dbName, String tableName, Record record,List<Column> cols);
    void deleteRecord(String dbName, String tableName, Record record);
    void updateRecord(String dbName, String tableName, Record record);
    Map<String,String> findAll(String dbName, String tableName);
    Map<String,String> findAllRecords(String name);
    String findRecordById(String id, String databaseTableNames);

    void deleteAllRecordsForTable(String dbName,String tableName);

    List<String> select(String dbName, String tableName, String condition, String columns) throws Exception;
    List<Map<String,String>> prepareJoinServ(String dbName, JoinReq joinReq,String condition);
    List<String> innerJoinServ(String dbName, JoinReq joinReq, String condition,String typeJoin);
    Map<String,String> groupByServ(String groupColName,String dbName,String table);


    List<String> hashJoin(String dbName, JoinReq joinReq, String a);
}
