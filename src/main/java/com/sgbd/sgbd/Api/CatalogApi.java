package com.sgbd.sgbd.Api;

import com.sgbd.sgbd.Model.Column;
import com.sgbd.sgbd.Model.TableReq;
import com.sgbd.sgbd.Service.Catalog;
import com.sgbd.sgbd.Service.exception.ExceptionType;
import com.sgbd.sgbd.Service.exception.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("catalog")
public class CatalogApi {

    private final Logger logger = LogManager.getLogger(CatalogApi.class);

    @Autowired
    private Catalog catalog;

    @RequestMapping(value = "/test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity test() {

        return new ResponseEntity(HttpStatus.OK);
    }
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/saveDb/{name}")
    public ResponseEntity saveDatabase(@PathVariable String name) {

        logger.info("LOG START - saveDatabase");


        catalog.saveDatabase(name);

        logger.info("LOG FINISH - saveDatabase");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @DeleteMapping(value = "/dropDb/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity dropDatabase(@PathVariable("name") String name) {

        logger.info("LOG START - dropDatabase");

        try {
            catalog.dropDatabase(name);
        }
         catch (ServiceException err){
            throw new ServiceException("This database doesn't exist", ExceptionType.DATABASE_NOT_EXISTS, HttpStatus.BAD_REQUEST);

        }

        logger.info("LOG FINISH - dropDatabase");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @DeleteMapping(value = "/dropTable/{nameDb}/{nameTable}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity dropTable(@PathVariable String nameDb,@PathVariable  String nameTable ){

        logger.info("LOG START - dropDatabase");

        try {
            catalog.dropTable(nameDb, nameTable);
        }
        catch (ServiceException ex){
            throw new ServiceException("There is no table in this database with this name ",ExceptionType.DATABASE_OR_TABLE_NOT_EXISTS,HttpStatus.BAD_REQUEST);
        }

        logger.info("LOG FINISH - dropDatabase");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value="/databases")
    public List<String> getAllDatabase(){
        return catalog.getAllDatabase();
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/saveTable/{dbName}/{tableName}", produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity saveTable(@PathVariable String dbName, @PathVariable  String tableName, @RequestBody TableReq columns){

        logger.info("LOG START - saveTable");


        catalog.saveTable(dbName, tableName, null, "", columns.getCols());

        logger.info("LOG FINISH - saveTable");
        return new ResponseEntity(HttpStatus.OK);
    }

    @ExceptionHandler({ServiceException.class})
    public ResponseEntity<ExceptionType> handleException(ServiceException exception) {

        return new ResponseEntity<>(exception.getType(), new HttpHeaders(), exception.getHttpStatus());
    }
}
