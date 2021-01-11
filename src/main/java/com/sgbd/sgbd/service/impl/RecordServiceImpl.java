package com.sgbd.sgbd.service.impl;

import com.sgbd.sgbd.model.*;
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

    private String formatValue(String value) {
        String result = "";
        String[] values = value.split("#");
        for (int i = 0; i < values.length - 1; i++) {
            result = result + values[i] + ";";
        }
        result += values[values.length - 1];
        return result;

    }

    @Override
    public List<Map<String, String>> prepareJoinServ(String dbName, JoinReq joinReq, String condition) {

        /*  condition:tabela.numeCol=valANDtabela2.numec2=val2*/

        Map<String, String> table1Rec = new HashMap<>();
        Map<String, String> table2Rec = new HashMap<>();

        List<String> resultList = new ArrayList<>();
        String table1 = joinReq.getTable1();
        String table2 = joinReq.getTable2();
        String col1 = joinReq.getCol1();
        String col2 = joinReq.getCol2();
        if (!condition.equals("")) {
            String[] conds = condition.split("AND");
            List<String> tableAndColList = new ArrayList<>();
            String table = "";
            for (String cond : conds) {
                String[] colAndVal = cond.split("=");
                String col = colAndVal[0];
                String val = colAndVal[1];
                String[] tableAndCol = col.split("\\.");
                String column = tableAndCol[1];
                table = tableAndCol[0];
                tableAndColList.add(table + ":" + column + ":" + val);
            }
            Map<String, String> finalRes = new HashMap<>();
            List<String> condWitoutIndexList = new ArrayList<>();
            for (String tableAndCol : tableAndColList) {
                String[] tableAndColTokens = tableAndCol.split(":");
                String tableName = tableAndColTokens[0];
                String colName = tableAndColTokens[1];
                String val = tableAndColTokens[2];
                Map<String, String> allRecordsMap = findAllRecords(dbName + "_" + tableName + "_" + colName + "Ind");
                if (!allRecordsMap.isEmpty()) {
                    //index exist
                    if (finalRes.size() == 0) {
                        // there is no cond with index yet
                        System.out.println("first index from condition");
                        Map<String, String> a = getAllRecordsForValue(dbName + "_" + tableName + "_" + colName + "Ind", val, dbName + "_" + tableName);
//                        finalRes.put(a.keySet())
                        System.out.println("final rez");
                        System.out.println(finalRes);
                    } else {

                        //intersectie
                        System.out.println("another index file");
                        Map<String, String> anotherIndexRez = getAllRecordsForValue(dbName + "_" + tableName + "_" + colName + "Ind", val, dbName + "_" + tableName);
                        System.out.println(anotherIndexRez);
                        System.out.println("aici");
//                        for (Map.Entry<String,String>entry:anotherIndexRez.entrySet()){
//                            if ()
//                        }
                    }
                } else {
                    System.out.println("no index for cond");
                    condWitoutIndexList.add(tableAndCol);

                }
            }

            if (condWitoutIndexList.size() != 0 && finalRes.size() != 0) {
                //there are some conditions in where which have no index file
                System.out.println("one with index and one without index");
                finalRes = applyConditionNoIndex(finalRes, condWitoutIndexList, dbName);
            }

            //get records for table which is not in the condition
            if (table.equals(table1)) {
                table2Rec = findAllRecords(dbName + "_" + table2);
                table1Rec = finalRes;
            } else {
                table1Rec = findAllRecords(dbName + "_" + table1);
                table2Rec = finalRes;

            }
            //kjdkd

            if (finalRes.size() == 0) {
                //no index
                System.out.println("no index for all cond");
                table1Rec = findAllRecords(dbName + "_" + table1);
                table2Rec = findAllRecords(dbName + "_" + table2);
                if (table.equals(table1)) {
                    table1Rec = applyConditionNoIndex(table1Rec, tableAndColList, dbName);
                } else {
                    table1Rec = applyConditionNoIndex(table1Rec, tableAndColList, dbName);
                }
            }


        }

//        selectJoin(listheader,val,selected)
        else {
            //no condition
            System.out.println("no condition");
            table1Rec = findAllRecords(dbName + "_" + table1);
            table2Rec = findAllRecords(dbName + "_" + table2);
        }

        List<Map<String, String>> tablesListFinal = new ArrayList<>();
        tablesListFinal.add(table1Rec);
        tablesListFinal.add(table2Rec);


        return tablesListFinal;

    }

    @Override
    public List<String> innerJoinServ(String dbName, JoinReq joinReq, String condition, String typeJoin) {

        List<String> resultList = new ArrayList<>();

        List<Map<String, String>> tableRecList = prepareJoinServ(dbName, joinReq, condition);
        Map<String, String> table1Rec = tableRecList.get(0);
        Map<String, String> table2Rec = tableRecList.get(1);

        for (Map.Entry<String, String> entry1 : table1Rec.entrySet()) {
            String val1 = findValueForColumn(entry1, joinReq.getCol1(), dbName, joinReq.getTable1());
            for (Map.Entry<String, String> entry2 : table2Rec.entrySet()) {
                String val2 = findValueForColumn(entry2, joinReq.getCol2(), dbName, joinReq.getTable2());
                if (typeJoin.equals("inner")) {
                    if (val1.equals(val2)) {
                        String s = entry1.getKey() + ";" + formatValue(entry1.getValue()) + ";" + entry2.getKey() + ";" + formatValue(entry2.getValue());
                        resultList.add(s);
                    }
                }
                if (typeJoin.equals("left")) {
                    if (val1.equals(val2)) {
                        String s = entry1.getKey() + ";" + entry1.getValue() + ";" + entry2.getKey() + ";" + entry2.getValue();
                        resultList.add(s);
                    } else {
                        String s = entry1.getKey() + ";" + entry1.getValue() + ";" + "null" + ";" + "null";
                        resultList.add(s);
                    }
                }
                if (typeJoin.equals("right")) {
                    if (val1.equals(val2)) {
                        String s = entry1.getKey() + ";" + entry1.getValue() + ";" + entry2.getKey() + ";" + entry2.getValue();
                        resultList.add(s);
                    } else {
                        String s = null + ";" + null + ";" + entry2.getKey() + ";" + entry2.getValue();
                        resultList.add(s);
                    }
                }

            }

        }
        String h = getHeaderTable(dbName, joinReq);
        System.out.println(h);

        List<String> select = selectJoin(h, resultList, joinReq.getSelectedColumns());
        System.out.println("select ***"+select);


        return select;
    }

    private String getHeaderTable(String dbName, JoinReq joinReq) {
        String header = "";
        List<String> headerList1 = catalogService.getAllColumnNameForTable(dbName, joinReq.getTable1());
        List<String> headerList2 = catalogService.getAllColumnNameForTable(dbName, joinReq.getTable2());
        for (String s : headerList1) {
            header += s + ";";
        }
        for (int i = 0; i < headerList2.size() - 1; i++) {
            header += headerList2.get(i) + ";";
        }
        header += headerList2.get(headerList2.size() - 1);
        return header;
    }

    private Map<String, String> applyConditionNoIndex(Map<String, String> table1Rec, List<String> tableAndColList, String dbName) {
        Map<String, String> recFinal = new HashMap<>();
        for (Map.Entry<String, String> rec : table1Rec.entrySet()) {
            for (String tableAndCol : tableAndColList) {
                String[] tableAndColTokens = tableAndCol.split(":");
                String tableName = tableAndColTokens[0];
                String colName = tableAndColTokens[1];
                String val = tableAndColTokens[2];
                String valRec = findValueForColumn(rec, colName, dbName, tableName);
                if (valRec.equals(val)) {
                    recFinal.put(rec.getKey(), rec.getValue());
                }
            }
        }
        return recFinal;
    }

    private Map<String, String> getAllRecordsForValue(String indexFileName, String value, String dbTableName) {

        //get all records which have value "value" for column with data=allRecForColumn from indexFileNAme in index file

        Map<String, String> result = new HashMap<>();
        Map<String, String> allRecords = findAllRecords(dbTableName);
        String recordForValue = findRecordById(value, indexFileName);
        if (recordForValue == null) {
            //don't exist records for condition
            throw new ServiceException("Don't exist records for condition", ExceptionType.ERROR, HttpStatus.BAD_REQUEST);
        } else {
            //exist records
            String[] ids = recordForValue.split(";");
            for (String id : ids) {
                String valueRec = allRecords.get(id);
                result.put(id, valueRec);
            }
        }
        System.out.println(result);
        return result;

    }

    @Override
    public List<String> hashJoin(String dbName, JoinReq joinReq, String selectedColumns) {

        /*
        let A = the first input table (or ideally, the larger one)
        let B = the second input table (or ideally, the smaller one)
        let jA = the join column ID of table A
        let jB = the join column ID of table B
        let MB = a multimap for mapping from single values to multiple rows of table B (starts out empty)
        let C = the output table (starts out empty)

        for each row b in table B:
           place b in multimap MB under key b(jB)

        for each row a in table A:
           for each row b in multimap MB under key a(jA):
              let c = the concatenation of row a and row b
              place row c in table C
         */
        String table1 = joinReq.getTable1();
        String table2 = joinReq.getTable2();
        String jA = joinReq.getCol1();
        String jB = joinReq.getCol2();
        Map<String, String> table1Rec = findAllRecords(dbName + "_" + table1);
        Map<String, String> table2Rec = findAllRecords(dbName + "_" + table2);

        Map<String, String> MB = new HashMap();
        Map<String, String> C = new HashMap<>();

        for (Map.Entry<String, String> entry : table2Rec.entrySet()) {
            MB.put(findValueForColumn(entry, jB, dbName, joinReq.getTable2()), entry.getValue());
        }

        for (Map.Entry<String, String> a : table1Rec.entrySet()) {
            String keyTable1 = findValueForColumn2(a, jA, dbName, joinReq.getTable1());
            boolean merged = false;
            for (Map.Entry<String, String> b : MB.entrySet()) {
                String c = a.getValue() + b.getValue();

                String keyTable2 = findValueForColumn2(b, jB, dbName, joinReq.getTable2());
                if (keyTable1 != null && keyTable2 != null && keyTable1.equals(keyTable2)) {
                    C.put(keyTable1, c);
                    merged = true;
                }
            }
            if (!merged) {
                C.put(keyTable1, a.getValue());
            }
        }

        String tableHeaders = "CustomerName;ContactName;Country;OrderDate"; // TODO: find all columns

        List<String> array = new ArrayList<String>(C.values());

        for (int i = 0; i < array.size(); i++) {
            String oldValue = array.get(i);
            String newValue = oldValue.replace('#', ';');
            array.set(i, newValue);
        }
        return selectJoin(tableHeaders, array, selectedColumns);
    }


    /*
    expected:
    colsHeader = email;nume;phone
    records[i] = 1;2;3
    selectedHeaders = email;phone

    returned:
    list[i] =  1;3
     */
    private List<String> selectJoin(String colsHeader, List<String> records, String selectedHeaders) {

        List<String> result = new ArrayList<>();
        String[] allHeaders = colsHeader.split(";");
        String[] selHeaders = selectedHeaders.split(";");

        for (String rec : records) {
            String selectedRec = "";
            String[] recTokens = rec.split(";");

            if(selectedHeaders.equals("*")) {
                selectedHeaders = colsHeader;
                selHeaders = selectedHeaders.split(";");
            }
            for (int i = 0; i < allHeaders.length; i++) { // loop through all columns
                String selectedCol = allHeaders[i];
                if (Arrays.asList(selHeaders).contains(selectedCol)) { // this is a selected header so i extract the values
                    selectedRec = selectedRec + recTokens[i] + ";";    // append value for selected header
                }
            }
            if (!selectedRec.equals("")) { // remove last ";" so it's "1;3" and not "1;3;"
                selectedRec = RecordServiceImpl.removeLastChar(selectedRec);
            }
            result.add(selectedRec);
        }

        return result;
    }

    private String findValueForColumn2(Map.Entry<String, String> entry1, String colName, String dbName, String table1) {

        /*
        I duplicated this function because i don't know what impact adding 'if(ind==-1) return null;' has
         */

        String[] values = entry1.getValue().split("#");
        int ind = findColumnInd(colName, dbName, table1);
        if (ind == -1)
            return null;
        String val;
        if (ind == 0) {
            val = entry1.getKey();
        } else {
            val = values[ind - 1];
        }
        return val;


    }

    private static String removeLastChar(String str) {
        return removeLastChars(str, 1);
    }

    private static String removeLastChars(String str, int chars) {
        return str.substring(0, str.length() - chars);
    }
}
