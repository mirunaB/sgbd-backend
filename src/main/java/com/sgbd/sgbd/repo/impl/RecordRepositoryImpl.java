package com.sgbd.sgbd.repo.impl;

import com.sgbd.sgbd.model.Index;
import com.sgbd.sgbd.model.Record;
import com.sgbd.sgbd.repo.RecordRepository;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class RecordRepositoryImpl implements RecordRepository {

    private RedisTemplate<String, String> redisTemplate;
    private HashOperations hashOperations;

    private static final String DATABASE_TABLE_SEPARATOR = "_";

    public RecordRepositoryImpl(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
        hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void save(String dbName, String tableName, Record record) {
        String key = record.getRow().entrySet().iterator().next().getKey();
        String value = record.getRow().get(key);
        hashOperations.put(dbName + DATABASE_TABLE_SEPARATOR + tableName, key, value);
    }

    @Override
    public void saveRecord(Record record, String dbTable) {
        String key = record.getRow().entrySet().iterator().next().getKey();
        String value = record.getRow().get(key);
        hashOperations.put(dbTable, key, value);
    }

    @Override
    public Map findAll(String dbName, String tableName) {
        return hashOperations.entries(dbName + DATABASE_TABLE_SEPARATOR + tableName);
    }

    @Override
    public Map<String, String> findAllRecords(String name) {
        return hashOperations.entries(name);
    }

    @Override
    public Record findById(String dbName, String tableName, Set<String> id) {
        return (Record) hashOperations.get(dbName + DATABASE_TABLE_SEPARATOR + tableName, id);
    }

    @Override
    public String findRecordById(String id, String databaseTableNames) {
        return (String)hashOperations.get(databaseTableNames, id);
    }

    @Override
    public void update(String dbName, String tableName, Record record) {
        save(dbName, tableName, record);
    }

    @Override
    public void delete(String dbName, String tableName, Record record) {
        String key = record.getRow().entrySet().iterator().next().getKey();
        String value = record.getRow().get(key);
        hashOperations.delete(dbName + DATABASE_TABLE_SEPARATOR + tableName, key, value);
    }

    @Override
    public void deleteRec(String dbTableName, Record record) {

    }

    private boolean checkCondition(Map.Entry<String, String> entry, List<String> condArr){

        if(condArr.size() == 0){
            return true;
        }
        String[] tokens = entry.getValue().split("#");
        int colIndex = Integer.valueOf(tokens[0]);

        for (String condition: condArr) {
            String[] condTokens = condition.split("#");
            int condTokenIndex = Integer.valueOf(condTokens[0]);
            String condTokensValue = condTokens[1];

            if(condTokenIndex == 0) { // we compare to key
                if(!entry.getKey().equals(condTokensValue)) {
                    return false;
                }
            }
            else {
                String value = tokens[condTokenIndex-1];
                if( !value.equals(condTokensValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public List<String> select(String dbName, String tableName, List<String> condition, List<Integer> columns, List<String> columnsToLookForIndex) {

        Map<String,String> records = null;
        for (String condWithName: columnsToLookForIndex) {
            records = this.findAllRecords(dbName+"_"+tableName+"_"+ condWithName +"Ind");

            Map<String,String> result = new HashMap<>();
            String res = "";
            for (Map.Entry<String, String> rec:records.entrySet()) { // iterate over all records we just retrieved

                String val = this.findRecordById(rec.getKey(), dbName + DATABASE_TABLE_SEPARATOR + tableName);
                result.put(rec.getKey(), val);

            }

            if(records != null && records.size()>0) { // index found
                break;
            }
        }

        if(records == null || records.size() == 0) { // in case we have no index retrieve all records
            records = this.findAllRecords(dbName + DATABASE_TABLE_SEPARATOR + tableName);
        }

        List<Map.Entry<String, String>> collect = records.entrySet().stream()
                .filter(entry -> checkCondition(entry, condition))
                .collect(Collectors.toList());

        // let's filter desired columns for each record
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> recordMap: collect) {

            String key = recordMap.getKey();
            String value = recordMap.getValue();
            String []tokens = value.split("#");

            String record = "";

            for (int col: columns) {
                if(col == 0){
                    record+= key + "#";
                }
                else{
                    record+=tokens[col-1];
                }
            }

            result.add(record);
        }

        return result;
    }
}
