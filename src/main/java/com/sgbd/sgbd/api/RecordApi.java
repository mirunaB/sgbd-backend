package com.sgbd.sgbd.api;

import com.sgbd.sgbd.model.Column;
import com.sgbd.sgbd.model.Pair;
import com.sgbd.sgbd.model.Record;
import com.sgbd.sgbd.service.CatalogService;
import com.sgbd.sgbd.service.RecordService;
import com.sgbd.sgbd.service.exception.ExceptionType;
import com.sgbd.sgbd.service.exception.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("record")
public class RecordApi {

    private final Logger logger = LogManager.getLogger(RecordApi.class);

    @Autowired
    private RecordService recordService;

    @Autowired
    private CatalogService catalogService;


    @RequestMapping(value = "/test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity test() {

        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/saveRecord/{dbName}/{tableName}", produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity saveRecord(@PathVariable String dbName, @PathVariable String tableName, @RequestBody Record record) {

        logger.info("LOG START - saveRecord");

        try {
            List<Column>cols=catalogService.getAllColumnForTable(dbName,tableName);
            recordService.saveRecord(dbName, tableName, record,cols);

        }
        catch (ServiceException ex){
            throw new ServiceException("unique",ExceptionType.DATABASE_OR_TABLE_NOT_EXISTS,HttpStatus.BAD_REQUEST);

        }
        //checkInsert(dbName,tableName,record);

        logger.info("LOG FINISH - saveRecord");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @DeleteMapping(value = "/deleteRecord/{dbName}/{tableName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteRecord(@PathVariable String dbName, @PathVariable String tableName, @RequestBody Record record) {

        logger.info("LOG START - deleteRecord");


        if (checkDel(dbName,tableName, record)==true){
            recordService.deleteRecord(dbName, tableName, record);
        }
        else{
            throw new ServiceException("Cannot delete this record",ExceptionType.ERROR,HttpStatus.BAD_REQUEST);
        }

        logger.info("LOG FINISH - deleteRecord");
        return new ResponseEntity(HttpStatus.OK);
    }

    public boolean checkDel(String dbName,String tableName,Record record){
        Map<String,String> allTables=catalogService.getAnotherTablesForDb(dbName,tableName);
        for (String table:allTables.keySet()){
            List<Column> cols=catalogService.getAllColumnForTable(dbName,table);
            for (int i=0;i<cols.size();i++){
                Map<String,String> fks=cols.get(i).getFKeys();
                if (fks!=null){
                    for (String key:fks.keySet()){
                        if (fks.get(key).contains(tableName)){
                            System.out.println("!!!!!!!!!"+cols.get(i).getAttributeName()+"_"+dbName+"_"+table+cols.get(i).getAttributeName()+"Ind");
                            Map<String,String> records=recordService.findAllRecords(dbName+"_"+table+cols.get(i).getAttributeName()+"Ind");
                            if (records.containsKey(record.getRow().keySet())){
                                return false;
                            }
                        }
                    }
                }
            }

        }
        return true;
    }


    @CrossOrigin(origins = "http://localhost:3000")
    @PutMapping(value = "/updateRecord/{dbName}/{tableName}/{record}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateRecord(@PathVariable String dbName, @PathVariable String tableName, @PathVariable Record record){

        logger.info("LOG START - updateRecord");

        recordService.updateRecord(dbName, tableName, record);

        logger.info("LOG FINISH - updateRecord");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value = "/findAll/{dbName}/{tableName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map> findAll(@PathVariable String dbName, @PathVariable String tableName){

        logger.info("LOG START - find all");

        Map result = recordService.findAll(dbName, tableName);

        logger.info("LOG FINISH - find all");
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value = "/findAllRec/{dbName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map> findAllRecords(@PathVariable String dbName){

        logger.info("LOG START - find all");

        Map result = recordService.findAllRecords(dbName);

        logger.info("LOG FINISH - find all");
        return new ResponseEntity<>(result, HttpStatus.OK);
    }



    @ExceptionHandler({ServiceException.class})
    public ResponseEntity<ExceptionType> handleException(ServiceException exception) {

        return new ResponseEntity<>(exception.getType(), new HttpHeaders(), exception.getHttpStatus());
    }
}
