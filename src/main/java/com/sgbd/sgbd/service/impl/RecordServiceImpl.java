package com.sgbd.sgbd.service.impl;

import com.sgbd.sgbd.model.Column;
import com.sgbd.sgbd.model.JoinReq;
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

import java.util.*;

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

        String key = (String) record.getRow().keySet().toArray()[0];
        String value = record.getRow().get(key);
        int nrKeys = key.split("#").length;
        String attributes = "";
        for (int i = 0; i < nrKeys; i++) {
            attributes += cols.get(i).getAttributeName();
        }

        String file = dbName + "_" + tableName + "_" + attributes + "Ind";
        String fileRec = dbName + "_" + tableName;

        List<Pair> uniqueKeys = new ArrayList<>();
        List<Pair> foreignKeys = new ArrayList<>();

        for (int i = nrKeys; i < cols.size(); i++) {
            String attributeName = cols.get(i).getAttributeName();
            String valueForAttribute = value.split("#")[i - nrKeys];
            if (cols.get(i).getIsUniqueKey() == true) {
                String uniqueKeyIndexFile = dbName + "_" + tableName + "_" + attributeName + "Ind";
                Map<String, String> allRecordsFromUKIndex = recordRepository.findAllRecords(uniqueKeyIndexFile);
                if (allRecordsFromUKIndex.keySet().stream().anyMatch(unique -> unique.equals(valueForAttribute))) {
                    throw new ServiceException("must be unique", ExceptionType.ERROR, HttpStatus.BAD_REQUEST);
                } else {
                    Map<String, String> map = new HashMap<>();
                    map.put(valueForAttribute, key);
                    uniqueKeys.add(new Pair(attributeName, new Record(map)));
                }
            }
            if (cols.get(i).getFKeys() != null) {
//                String keys=cols.get(i).getFKeys().keySet().toString();
                String cheie = cols.get(i).getFKeys().keySet().toArray()[0].toString();
                String valuesKeyFk = cols.get(i).getFKeys().get(cheie).split(",")[1];
                String cheieKeyFk = cols.get(i).getFKeys().get(cheie).split(",")[0];
                //aici e cu , si arunca exceptii cum trimit ele fk
                String parentPKIndexFile = dbName + "_" + cheieKeyFk + "_" + valuesKeyFk + "Ind";
                Map<String, String> allRecordsFromParentTablePKIndex = recordRepository.findAllRecords(parentPKIndexFile);
                boolean existsValueInParentTable = allRecordsFromParentTablePKIndex.containsKey(valueForAttribute);
                if (!existsValueInParentTable) {
                    throw new ServiceException("There is Fk Constraint", ExceptionType.ERROR, HttpStatus.BAD_REQUEST);
                } else {
                    String FKIndexFile = dbName + "_" + tableName + "_" + attributeName + "Ind";
                    Map<String, String> allRecordsFromFKIndex = recordRepository.findAllRecords(FKIndexFile);
                    String valueForCurrentFK = allRecordsFromFKIndex.get(valueForAttribute);
                    if (valueForCurrentFK == null) {
                        Map<String, String> newMap = new HashMap<>();
                        newMap.put(valueForAttribute, key);
                        foreignKeys.add(new Pair(attributeName, new Record(newMap)));
                    } else {
                        Map<String, String> newMap = new HashMap<>();
                        newMap.put(valueForAttribute, valueForCurrentFK + ";" + key);
                        foreignKeys.add(new Pair(attributeName, new Record(newMap)));
                    }
                }
            }
        }


        Map<String, String> recordMap = new HashMap();
        recordMap.put(key, value);

        recordRepository.saveRecord(new Record(recordMap), file);
        recordRepository.saveRecord(record, fileRec);
        uniqueKeys.forEach(pair -> {
            String uniqueKeyIndexFile = dbName + "_" + tableName + "_" + pair.getKey() + "Ind";
            recordRepository.saveRecord((Record) pair.getValue(), uniqueKeyIndexFile);
        });
        foreignKeys.forEach(pair -> {
            String FKIndexFile = dbName + "_" + tableName + "_" + pair.getKey() + "Ind";
            recordRepository.saveRecord((Record) pair.getValue(), FKIndexFile);
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
    public Map<String, String> findAll(String dbName, String tableName) {

        return recordRepository.findAll(dbName, tableName);
    }

    @Override
    public Map<String, String> findAllRecords(String name) {
        return recordRepository.findAllRecords(name);
    }

    @Override
    public String findRecordById(String id, String databaseTableNames) {
        return recordRepository.findRecordById(id, databaseTableNames);
    }

    @Override
    public void deleteAllRecordsForTable(String dbName, String tableName) {
        Map<String, String> mymap = new HashMap();
        mymap = findAll(dbName, tableName);
        mymap.forEach((k, v) -> {
            Record r = new Record();
            Map<String, String> row = new HashMap<>();
            row.put(k, v);
            r.setRow(row);
            deleteRecord(dbName, tableName, r);
        });
    }

    private int findColumnIndexInTable(String searchedColumn, String dbName, String tableName, boolean skipAsterisk) {

        List<Column> cols = catalogService.getAllColumnForTable(dbName, tableName);

        for (int j = 0; j < cols.size(); j++) {
            if (skipAsterisk || searchedColumn.equals(cols.get(j).getAttributeName())) {
                return j;
            }
        }

        return -1;
    }

    @Override
    public List select(String dbName, String tableName, String condition, String columns) throws Exception {

        // let's translate $columns into indexes using $cols; in repo we store them as col1#col2#col3... so if we send an array of numbers
        // we can split by # and select desired columns
        List<Integer> colsIndex = new ArrayList<>();
        String[] columnsTokens = new String[10000];
        String[] aux = columns.split(",");

        int auxSelect = 0;
        for (String caux : aux) {
            columnsTokens[auxSelect] = caux;
            auxSelect++;
        }
        List<String> conditionWithName = new ArrayList<>();


        if (columns.equals("*")) {
            List<Column> colList = catalogService.getAllColumnForTable(dbName, tableName);

            for (int i = 0; i < colList.size(); i++) {
                columnsTokens[i] = String.valueOf(colList.get(i).getAttributeName());
                conditionWithName.add(columnsTokens[i]);
                colsIndex.add(i);
            }
        } else {
            int size = columnsTokens.length;
            for (int i = 0; i < auxSelect; i++) {
                int index = -1;
                if (!columns.equals("*")) {
                    index = findColumnIndexInTable(columnsTokens[i], dbName, tableName, columns.equals("*"));
                    conditionWithName.add(columnsTokens[i]);
                } else {
                    index = i;
                }
                if (index != -1) {
                    colsIndex.add(index);
                } else {
                    throw new Exception("invalid column");
                }
            }
        }

        // let's do same thing for condition
        List<String> conditions = new ArrayList<>();
        String[] tokens = condition.split("AND");
        if (!condition.equals("") && !condition.equals("undefined")) {
            for (String cond : tokens) {
                // i assume this cond looks like colName=value
                String[] condTokens = cond.split("=");
                String colummName = condTokens[0];
                String value = condTokens[1];
                int columnIndex = findColumnIndexInTable(colummName, dbName, tableName, false);
                conditions.add(columnIndex + "#" + value);
            }
        }

        return recordRepository.select(dbName, tableName, conditions, colsIndex, conditionWithName);
    }

    private int findColumnInd(String searchedColumn, String dbName, String tableName) {

        List<Column> cols = catalogService.getAllColumnForTable(dbName, tableName);

        for (int j = 0; j < cols.size(); j++) {
            if (searchedColumn.equals(cols.get(j).getAttributeName())) {
                return j;
            }
        }

        return -1;
    }

    private String findValueForColumn(Map.Entry<String, String> entry1, String colName, String dbName, String table1) {

        /*find value for a column from it's value map(id:col1#col2)*/

        String[] values = entry1.getValue().split("#");
        int ind = findColumnInd(colName, dbName, table1);
        String val;
        if (ind == 0) {
            val = entry1.getKey();
        } else {
            val = values[ind - 1];
        }
        return val;


    }

    private String formatValue(String value){
        String result="";
        String[] values = value.split("#");
        for (int i=0;i<values.length-1;i++){
            result=result+values[i]+";";
        }
        result+=values[values.length-1];
        return result;

    }

    @Override
    public List<String> nestedJoinServ(String dbName, JoinReq joinReq) {


        List<String> resultList=new ArrayList<>();
        String table1 = joinReq.getTable1();
        String table2 = joinReq.getTable2();
        String col1 = joinReq.getCol1();
        String col2 = joinReq.getCol2();
        Map<String, String> table1Rec = findAllRecords(dbName + "_" + table1);
        Map<String, String> table2Rec = findAllRecords(dbName + "_" + table2);
        for (Map.Entry<String, String> entry1 : table1Rec.entrySet()) {
            String val1=findValueForColumn(entry1,col1, dbName,table1);
            for (Map.Entry<String, String> entry2 : table2Rec.entrySet()) {
                String val2= findValueForColumn(entry2,col2, dbName,table2);
                if (val1.equals(val2)){
                    String s=entry1.getKey()+";"+formatValue(entry1.getValue())+";"+entry2.getKey()+";"+formatValue(entry2.getValue());
                    resultList.add(s);
                }
            }

        }

       return resultList;

    }

    @Override
    public List<String> leftNestedJoinServ(String dbName, JoinReq joinReq) {
        List<String> resultList=new ArrayList<>();
        String table1 = joinReq.getTable1();
        String table2 = joinReq.getTable2();
        String col1 = joinReq.getCol1();
        String col2 = joinReq.getCol2();
        Map<String, String> table1Rec = findAllRecords(dbName + "_" + table1);
        Map<String, String> table2Rec = findAllRecords(dbName + "_" + table2);
        for (Map.Entry<String, String> entry1 : table1Rec.entrySet()) {
            String val1=findValueForColumn(entry1,col1, dbName,table1);
            for (Map.Entry<String, String> entry2 : table2Rec.entrySet()) {
                String val2= findValueForColumn(entry2,col2, dbName,table2);
                if (val1.equals(val2)){
                    String s=entry1.getKey()+";"+entry1.getValue()+";"+entry2.getKey()+";"+entry2.getValue();
                    resultList.add(s);
                }
                else{
                    String s=entry1.getKey()+";"+entry1.getValue()+";"+"null"+";"+"null";
                    resultList.add(s);
                }
            }

        }

        return resultList;

    }

    @Override
    public List<String> rightNestedJoinServ(String dbName, JoinReq joinReq) {
        List<String> resultList=new ArrayList<>();
        String table1 = joinReq.getTable1();
        String table2 = joinReq.getTable2();
        String col1 = joinReq.getCol1();
        String col2 = joinReq.getCol2();
        Map<String, String> table1Rec = findAllRecords(dbName + "_" + table1);
        Map<String, String> table2Rec = findAllRecords(dbName + "_" + table2);
        for (Map.Entry<String, String> entry1 : table1Rec.entrySet()) {
            String val1=findValueForColumn(entry1,col1, dbName,table1);
            for (Map.Entry<String, String> entry2 : table2Rec.entrySet()) {
                String val2= findValueForColumn(entry2,col2, dbName,table2);
                if (val1.equals(val2)){
                    String s=entry1.getKey()+";"+entry1.getValue()+";"+entry2.getKey()+";"+entry2.getValue();
                    resultList.add(s);
                }
                else{
                    String s=null+";"+null+";"+entry2.getKey()+";"+entry2.getValue();
                    resultList.add(s);
                }
            }

        }

        return resultList;
    }

    /*
    expected:
    colsHeader = email;nume;phone
    records[i] = 1;2;3
    selectedHeaders = email;phone

    returned:
    list[i] =  1;3
     */
    private List<String> selectJoin(String colsHeader, List<String> records, String selectedHeaders){

        List<String> result = new ArrayList<>();
        String[] allHeaders = colsHeader.split(";");
        String[] selHeaders = selectedHeaders.split(";");

        for (String rec: records) {
            String selectedRec = "";
            String[] recTokens = rec.split(";");

            for (int i=0; i<selHeaders.length; i++) { // loop through all columns
                String selectedCol= selHeaders[i];
                if(Arrays.asList(allHeaders).contains(selectedCol)) { // this is a selected header so i extract the values
                    selectedRec = selectedRec + recTokens[i] + ";";    // append value for selected header
                }
            }
            if(!selectedRec.equals("")) { // remove last ";" so it's "1;3" and not "1;3;"
                RecordServiceImpl.removeLastChar(selectedRec);
            }
            result.add(selectedRec);
        }

        return result;
    }

    private static String removeLastChar(String str) {
        return removeLastChars(str, 1);
    }

    private static String removeLastChars(String str, int chars) {
        return str.substring(0, str.length() - chars);
    }
}
