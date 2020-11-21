package com.sgbd.sgbd.api;

import com.sgbd.sgbd.model.Column;
import com.sgbd.sgbd.model.Index;
import com.sgbd.sgbd.model.TableReq;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("catalog")
public class CatalogApi {

    private final Logger logger = LogManager.getLogger(CatalogApi.class);

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private RecordService recordService;

    @RequestMapping(value = "/test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity test() {

        return new ResponseEntity(HttpStatus.OK);
    }
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/saveDb/{name}")
    public ResponseEntity saveDatabase(@PathVariable String name) {

        logger.info("LOG START - saveDatabase");

        catalogService.saveDatabase(name);

        logger.info("LOG FINISH - saveDatabase");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @DeleteMapping(value = "/dropDb/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity dropDatabase(@PathVariable("name") String name) {

        logger.info("LOG START - dropDatabase");

        try {
            catalogService.dropDatabase(name);
        } catch (ServiceException err) {
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
            catalogService.dropTable(nameDb, nameTable);
            //delete all records for that table
            recordService.deleteAllRecordsForTable(nameDb,nameTable);
        }
        catch (ServiceException ex){
            throw new ServiceException("There is no table in this database with this name ",ExceptionType.DATABASE_OR_TABLE_NOT_EXISTS,HttpStatus.BAD_REQUEST);
        }
        logger.info("LOG FINISH - dropDatabase");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/saveTable/{dbName}/{tableName}", produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity saveTable(@PathVariable String dbName, @PathVariable  String tableName, @RequestBody Column[] columns){

        logger.info("LOG START - saveTable");

        catalogService.saveTable(dbName, tableName, null, "", columns);
        catalogService.addIndexForTable(dbName,tableName,columns);

        logger.info("LOG FINISH - saveTable");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(value = "/addIndex/{dbName}/{tableName}", produces = MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addIndex(@PathVariable String dbName, @PathVariable  String tableName, @RequestBody Index index){

        logger.info("LOG START - add Index");

        catalogService.addIndex(dbName, tableName,index);

        logger.info("LOG FINISH - Add index");
        return new ResponseEntity(HttpStatus.OK);
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value="/cols/{dbName}/{tableName}")
    public List<String> getAllNameColsForTable(@PathVariable String dbName,@PathVariable String tableName){

       /** get all cols' name for a table*/

        logger.info("LOG START - get all name cols");

        List<String> allDatabase = catalogService.getAllColumnNameForTable(dbName,tableName);

        logger.info("LOG FINISH - get all name cols");
        return allDatabase;
    }

    //get all cols for a table
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value="/allCols/{dbName}/{tableName}")
    public List<Column> getAllColsForTable(@PathVariable String dbName,@PathVariable String tableName){

        logger.info("LOG START - get all cols");

        List<Column> allDatabase = catalogService.getAllColumnForTable(dbName,tableName);

        logger.info("LOG FINISH - get all cols");
        return allDatabase;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value="/databases")
    public List<String> getAllDatabase(){

        logger.info("LOG START - getAllDatabases");
        String dbName = "facultate";
        String tableName = "cursuri";
        String condition = "nume=nume";
        String[] columns = new String[2];
        columns[0] = "id";
        columns[1] = "nume";

        // TODO: delete this; testing
        List<String> result = recordService.select(dbName, tableName, condition, columns);

        List<String> allDatabase = catalogService.getAllDatabase();

        logger.info("LOG FINISH - getAllDatabases");
        return allDatabase;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value="/tables/{dbName}")
    public List<String> getAllTablesForDb(@PathVariable String dbName){

        logger.info("LOG START - getAllTablesForDb");

        List<String> allTables = catalogService.getAllTablesForDb(dbName);

        logger.info("LOG FINISH - getAllTablesForDb");
        return allTables;
    }

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping(value="/tables/{dbName}/{tableName}")
    public Map<String,String> getTablesForDb(@PathVariable String dbName, @PathVariable String tableName){

        logger.info("LOG START - getAllTablesForDb");

        Map<String,String> allTables = catalogService.getAnotherTablesForDb(dbName,tableName);

        logger.info("LOG FINISH - getAllTablesForDb");
        return allTables;
    }

    @ExceptionHandler({ServiceException.class})
    public ResponseEntity<ExceptionType> handleException(ServiceException exception) {

        return new ResponseEntity<>(exception.getType(), new HttpHeaders(), exception.getHttpStatus());
    }
}
