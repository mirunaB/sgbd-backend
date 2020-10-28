package com.sgbd.sgbd.api;

import com.sgbd.sgbd.model.Record;
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

import java.util.Map;

@RestController
@RequestMapping("record")
public class RecordApi {

    private final Logger logger = LogManager.getLogger(RecordApi.class);

    @Autowired
    private RecordService recordService;


    @RequestMapping(value = "/test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity test() {

        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/saveRecord/{dbName}/{tableName}", produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity saveRecord(@PathVariable String dbName, @PathVariable String tableName, @RequestBody Record record) {

        logger.info("LOG START - saveRecord");

        recordService.saveRecord(dbName, tableName, record);

        logger.info("LOG FINISH - saveRecord");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @DeleteMapping(value = "/deleteRecord/{dbName}/{tableName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteRecord(@PathVariable String dbName, @PathVariable String tableName, @RequestBody Record record) {

        logger.info("LOG START - deleteRecord");

        recordService.deleteRecord(dbName, tableName, record);

        logger.info("LOG FINISH - deleteRecord");
        return new ResponseEntity(HttpStatus.OK);
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



    @ExceptionHandler({ServiceException.class})
    public ResponseEntity<ExceptionType> handleException(ServiceException exception) {

        return new ResponseEntity<>(exception.getType(), new HttpHeaders(), exception.getHttpStatus());
    }
}
