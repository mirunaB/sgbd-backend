package com.sgbd.sgbd.Main;

import com.sgbd.sgbd.Model.Column;
import com.sgbd.sgbd.Service.Catalog;
import com.sgbd.sgbd.Service.CatalogImpl;


public class NonSpringBootTest {
    public static void main(String[] args) {

        Catalog catalog = new CatalogImpl();
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
