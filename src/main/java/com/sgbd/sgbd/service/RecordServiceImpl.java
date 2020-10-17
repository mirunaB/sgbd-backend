package com.sgbd.sgbd.service;

import com.sgbd.sgbd.model.Record;
import com.sgbd.sgbd.repo.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

@org.springframework.stereotype.Service
@Primary
@Component
public class RecordServiceImpl implements RecordService {

    @Autowired
    private RecordRepository recordRepository;

    @Override
    public void saveRecord(String dbName, String tableName, Record record) {

        recordRepository.save(dbName, tableName, record);
    }

    @Override
    public void deleteRecord(String dbName, String tableName, Record record) {

        recordRepository.delete(dbName, tableName, record);
    }

    @Override
    public void updateRecord(String dbName, String tableName, Record record) {

        recordRepository.update(dbName, tableName, record);
    }

    @Override
    public Map findAll(String dbName, String tableName) {

        return recordRepository.findAll(dbName, tableName);
    }
}
