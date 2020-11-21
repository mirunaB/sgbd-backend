package com.sgbd.sgbd.service.impl;

import com.sgbd.sgbd.model.Column;
import com.sgbd.sgbd.model.Pair;
import com.sgbd.sgbd.model.Record;
import com.sgbd.sgbd.repo.RecordRepository;
import com.sgbd.sgbd.service.RecordService;
import com.sgbd.sgbd.service.exception.ExceptionType;
import com.sgbd.sgbd.service.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
@Primary
@Component
public class RecordServiceImpl implements RecordService {


    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private CatalogServiceImpl catalogService;

    @Override
    public void saveRecord(String dbName, String tableName, Record record, List<Column> cols) {

        String key= (String) record.getRow().keySet().toArray()[0];
        String value=record.getRow().get(key);
        int nrKeys=key.split("#").length;
        String attributes = "";
        for (int i=0;i<nrKeys;i++){
            attributes +=cols.get(i).getAttributeName();
        }

        String file = dbName +"_"+tableName+ "_" + attributes + "Ind";
        String fileRec = dbName +"_"+tableName;

        List<Pair> uniqueKeys = new ArrayList<>();
        List<Pair> foreignKeys = new ArrayList<>();

        for(int i = nrKeys; i < cols.size(); i++){
            String attributeName = cols.get(i).getAttributeName();
            String valueForAttribute = value.split("#")[i-nrKeys];
            if(cols.get(i).getIsUniqueKey() == true){
                String uniqueKeyIndexFile = dbName+"_"+tableName + "_" + attributeName + "Ind";
                Map<String, String> allRecordsFromUKIndex = recordRepository.findAllRecords(uniqueKeyIndexFile);
                if(allRecordsFromUKIndex.keySet().stream().anyMatch(unique->unique.equals(valueForAttribute))){
                    throw new ServiceException("must be unique", ExceptionType.ERROR, HttpStatus.BAD_REQUEST);
                }
                else{
                    Map<String,String> map=new HashMap<>();
                    map.put(valueForAttribute, key);
                    uniqueKeys.add(new Pair(attributeName, new Record(map)));
                }
            }
            if (cols.get(i).getFKeys() != null){
//                String keys=cols.get(i).getFKeys().keySet().toString();
                String cheie=cols.get(i).getFKeys().keySet().toArray()[0].toString();
                String valuesKeyFk=cols.get(i).getFKeys().get(cheie).split(",")[1];
                String cheieKeyFk=cols.get(i).getFKeys().get(cheie).split(",")[0];
                //aici e cu , si arunca exceptii cum trimit ele fk
                String parentPKIndexFile = dbName + "_" + cheieKeyFk + "_" + valuesKeyFk + "Ind";
                Map<String, String> allRecordsFromParentTablePKIndex = recordRepository.findAllRecords(parentPKIndexFile);
                boolean existsValueInParentTable = allRecordsFromParentTablePKIndex.containsKey(valueForAttribute);
                if(!existsValueInParentTable){
                    throw new ServiceException("There is Fk Constraint", ExceptionType.ERROR, HttpStatus.BAD_REQUEST);
                }
                else{
                    String FKIndexFile = dbName+"_"+tableName + "_" + attributeName + "Ind";
                    Map<String, String> allRecordsFromFKIndex = recordRepository.findAllRecords(FKIndexFile);
                    String valueForCurrentFK = allRecordsFromFKIndex.get(valueForAttribute);
                    if(valueForCurrentFK == null){
                        Map<String, String> newMap=new HashMap<>();
                        newMap.put(valueForAttribute,key);
                        foreignKeys.add(new Pair(attributeName, new Record(newMap)));
                    }
                    else{
                        Map<String, String> newMap=new HashMap<>();
                        newMap.put(valueForAttribute,valueForCurrentFK + ";" + key);
                        foreignKeys.add(new Pair(attributeName, new Record(newMap)));
                    }
                }
            }
        }


        Map<String,String> recordMap=new HashMap();
        recordMap.put(key,value);

        recordRepository.saveRecord(new Record(recordMap), file);
        recordRepository.saveRecord(record,fileRec);
        uniqueKeys.forEach(pair -> {
            String uniqueKeyIndexFile = dbName+"_"+tableName + "_" + pair.getKey() + "Ind";
            recordRepository.saveRecord((Record)pair.getValue(), uniqueKeyIndexFile);
        });
        foreignKeys.forEach(pair -> {
            String FKIndexFile = dbName+"_"+tableName + "_" + pair.getKey() + "Ind";
            recordRepository.saveRecord((Record)pair.getValue(), FKIndexFile);
        });
//        recordRepository.save(dbName, tableName, record);
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
    public Map<String,String> findAll(String dbName, String tableName) {

        return recordRepository.findAll(dbName, tableName);
    }

    @Override
    public Map<String, String> findAllRecords(String name) {
        return recordRepository.findAllRecords(name);
    }

    @Override
    public String findRecordById(String id, String databaseTableNames) {
       return recordRepository.findRecordById(id,databaseTableNames);
    }

    @Override
    public void deleteAllRecordsForTable(String dbName, String tableName) {
        Map<String,String> mymap=new HashMap();
        mymap=findAll(dbName,tableName);
        mymap.forEach((k,v)->{
            Record r=new Record();
            Map<String,String> row=new HashMap<>();
            row.put(k,v);
            r.setRow(row);
            deleteRecord(dbName,tableName,r);
        });
    }

    private int findColumnIndexInTable(String searchedColumn, String dbName, String tableName){

        List<Column> cols = catalogService.getAllColumnForTable(dbName,tableName);

        for(int j=0; j<cols.size(); j++){
            if(searchedColumn.equals(cols.get(j).getAttributeName())){
                return j;
            }
        }

        return -1;
    }

    @Override
    public List<String> select(String dbName, String tableName, String condition, String[] columns) {

        // let's translate $columns into indexes using $cols; in repo we store them as col1#col2#col3... so if we send an array of numbers
        // we can split by # and select desired columns
        List<Integer> colsIndex = new ArrayList<>();
        for (int i=0; i<columns.length; i++){

            int index = findColumnIndexInTable(columns[i], dbName, tableName);
            if(index != -1){
                colsIndex.add(index);
            }
            else{
                // TODO: raise an exception; invalid column name
            }
        }

        // let's do same thing for condition
        List<String> conditions = new ArrayList<>();

        String[] tokens = condition.split("AND");
        for (String cond: tokens) {
            // i assume this cond looks like colName=value
            String[] condTokens = cond.split("=");
            String colummName = condTokens[0];
            String value = condTokens[1];

            int columnIndex = findColumnIndexInTable(colummName, dbName, tableName);
            conditions.add(columnIndex + "#" + value);
        }

        return recordRepository.select(dbName, tableName, conditions, colsIndex);
    }
}
