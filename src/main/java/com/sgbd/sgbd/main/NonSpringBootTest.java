package com.sgbd.sgbd.main;

import com.sgbd.sgbd.model.Column;
import com.sgbd.sgbd.service.CatalogService;
import com.sgbd.sgbd.service.CatalogImpl;


public class NonSpringBootTest {
    public static void main(String[] args) {

        CatalogService catalog = new CatalogImpl();
        catalog.saveDatabase("Radu");
        //catalog.dropDatabase("anjnaja") ;
        //catalog.dropTable("Radu", "mytable");

        Column column1 = Column.builder()
                .attributeName("Specid")
                .type("string")
                .length(20)
                .isNull(false)
                .isPrimaryKey(true)
                .isUniqueKey(true)
                .hasIndex(true)
                .indexName("SpecIdIndex")
                .keyLength(22)
                .isUnique(true)
                .indexType("BTree")
                .build();
        Column column2 = Column.builder()
                .attributeName("text")
                .type("string")
                .length(20)
                .isNull(false)
                .isPrimaryKey(false)
                .isUniqueKey(true)
                .hasIndex(false)
                .build();

        catalog.saveTable("Radu", "mytable", "table.txt", "12", column1, column2);
    }
}
