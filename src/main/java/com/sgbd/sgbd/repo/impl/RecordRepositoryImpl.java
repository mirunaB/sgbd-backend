package com.sgbd.sgbd.repo.impl;

import com.sgbd.sgbd.model.Record;
import com.sgbd.sgbd.repo.RecordRepository;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;

@Repository
public class RecordRepositoryImpl implements RecordRepository {

    private RedisTemplate<String, String> redisTemplate;
    private HashOperations hashOperations;

    private static final String DATABASE_TABLE_SEPARATOR = ".";

    public RecordRepositoryImpl(RedisTemplate<String, String> redisTemplate) {

        this.redisTemplate = redisTemplate;
        hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void save(String dbName, String tableName, Record record) {
        hashOperations.put(dbName +  DATABASE_TABLE_SEPARATOR + tableName, record.getPrimaryKeySet(), record);
    }

    @Override
    public Map findAll(String dbName, String tableName) {
        return hashOperations.entries(dbName + DATABASE_TABLE_SEPARATOR + tableName);
    }

    @Override
    public Record findById(String dbName, String tableName, Set<String> id) {
        return (Record)hashOperations.get(dbName + DATABASE_TABLE_SEPARATOR + tableName, id);
    }

    @Override
    public void update(String dbName, String tableName, Record record) {
        save(dbName, tableName, record);
    }

    @Override
    public void delete(String dbName, String tableName, Record record) {
        hashOperations.delete(dbName + DATABASE_TABLE_SEPARATOR + tableName, record);
    }
}
