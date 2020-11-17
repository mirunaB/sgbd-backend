package com.sgbd.sgbd.service.impl;

import com.sgbd.sgbd.model.Column;
import com.sgbd.sgbd.constants.XMLConstants;
import com.sgbd.sgbd.model.Index;
import com.sgbd.sgbd.service.CatalogService;
import com.sgbd.sgbd.service.exception.ExceptionType;
import com.sgbd.sgbd.service.exception.ServiceException;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Primary
@Component
public class CatalogServiceImpl implements CatalogService {

    public static final String FILE_NAME = "Catalog.xml";
    private DocumentBuilderFactory dbFactory;
    private DocumentBuilder dBuilder;
    private Document doc;
    private Node databasesElement;

    /**
     *
     */
    public CatalogServiceImpl() {

        this.loadXML();
        databasesElement = doc.getFirstChild();
    }

    /**
     * This function loads xml into doc
     */
    public void loadXML() {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        try {
            doc = dBuilder.parse(FILE_NAME);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param dbName - name of the db node you want to insert
     */
    public void saveDatabase(String dbName) {

        if (findDatabase(dbName)==true){
            throw new ServiceException("This database already exists",ExceptionType.DATABASE_ALREADY_EXISTS,HttpStatus.BAD_REQUEST);
        }

        Element tablesElement = doc.createElement(XMLConstants.TABLES_TAG);
        Element databaseElement = doc.createElement(XMLConstants.DATABASE_TAG);
        databaseElement.appendChild(tablesElement);
        databaseElement.setAttribute(XMLConstants.NAME_TAG, dbName);
        databasesElement.appendChild(databaseElement);
        submitChangesToFile();
    }

    public boolean findDatabase(String dbName){

        boolean isFind=false;
        NodeList nodeList = doc.getElementsByTagName(XMLConstants.DATABASE_TAG);

        for(int i=0; i<nodeList.getLength(); i++){
            Element currentElement = (Element)nodeList.item(i);
            if(currentElement.getAttribute(XMLConstants.NAME_TAG).equals(dbName)){
                isFind=true;
                break;
            }
        }

        return isFind;
    }

    /**
     * @param dbName
     */
    public void dropDatabase(String dbName) {

        NodeList nodeList = doc.getElementsByTagName(XMLConstants.DATABASE_TAG);

        int i;
        for(i=0; i<nodeList.getLength(); i++){
            Element currentElement = (Element)nodeList.item(i);
            if(currentElement.getAttribute(XMLConstants.NAME_TAG).equals(dbName)){
                currentElement.getParentNode().removeChild(currentElement);
            }
        }

        if (i==nodeList.getLength()){
            throw new ServiceException("This database doesn't exist", ExceptionType.DATABASE_NOT_EXISTS, HttpStatus.BAD_REQUEST);
        }

        submitChangesToFile();
    }

    /**
     * @param dbName
     * @param tableName
     * @param fileName
     * @param rowLength
     */
    public void saveTable(String dbName, String tableName, String fileName, String rowLength, Column[] columns) {


        Element structureElement = doc.createElement(XMLConstants.STRUCTURE_TAG);
        Element primaryKeyElement = doc.createElement(XMLConstants.PRIMARY_KEY_TAG);
        Element uniqueKeysElement = doc.createElement(XMLConstants.UNIQUE_KEYS_TAG);
        Element indexFilesElement = doc.createElement(XMLConstants.INDEX_FILES_TAG);
        Element fkElem=null;

        for (Column column:
             columns) {
            Element columnElement = doc.createElement(XMLConstants.ATTRIBUTE_TAG);
            columnElement.setAttribute(XMLConstants.ATTRIBUTE_NAME_TAG, column.getAttributeName());
            columnElement.setAttribute(XMLConstants.TYPE_TAG, column.getType());
            columnElement.setAttribute(XMLConstants.LENGTH_TAG, String.valueOf(column.getLength()));
            columnElement.setAttribute(XMLConstants.IS_NULL_TAG, String.valueOf(column.getIsNull()));

            structureElement.appendChild(columnElement);

            if(column.getIsPrimaryKey()){
                Element pkAttributeElement = doc.createElement(XMLConstants.PRIMARY_KEY_ATTRIBUTE_TAG);
                pkAttributeElement.setTextContent(column.getAttributeName());
                primaryKeyElement.appendChild(pkAttributeElement);
            }

            if(column.getIsUniqueKey()){

                Element uniqueAttributeElement = doc.createElement(XMLConstants.UNIQUE_ATTRIBUTE_TAG);
                uniqueAttributeElement.setTextContent(column.getAttributeName());
                uniqueKeysElement.appendChild(uniqueAttributeElement);
                List<Column> attributeList = new ArrayList<>();
                attributeList.add(column);
                Index index = new Index(column.getAttributeName() + "Ind", true, attributeList);
                addIndex( dbName, tableName,index);
            }

            if (column.getFKeys() !=null){
                //table has fk
                fkElem=setFkForColumn(column);
            }

        }

        Element tableElement = doc.createElement(XMLConstants.TABLE_TAG);
        tableElement.setAttribute(XMLConstants.TABLE_NAME_TAG, tableName);
        tableElement.setAttribute(XMLConstants.FILE_NAME_TAG, fileName);
        tableElement.setAttribute(XMLConstants.ROW_LENGTH_TAG, rowLength);

        tableElement.appendChild(structureElement);
        tableElement.appendChild(primaryKeyElement);
        tableElement.appendChild(uniqueKeysElement);
        if (fkElem!=null){
            tableElement.appendChild(fkElem);
        }
        tableElement.appendChild(indexFilesElement);

        NodeList nodeList = doc.getElementsByTagName(XMLConstants.TABLES_TAG);

        for(int i=0; i<nodeList.getLength(); i++){
            Element currentTablesElement = (Element)nodeList.item(i);
            Element database = (Element)currentTablesElement.getParentNode();

            if(database.getAttribute(XMLConstants.NAME_TAG).equals(dbName)){
                currentTablesElement.appendChild(tableElement);
            }
        }

        submitChangesToFile();
    }

    /** set foreign key for a table in xml file*/
    public Element setFkForColumn(Column column){

        Element fkElemParinte = doc.createElement(XMLConstants.FOREIGN_KEYS_TAG);
        for(String key:column.getFKeys().keySet()){
            Element fkElem = doc.createElement(XMLConstants.FOREIGN_KEY_TAG);
            fkElemParinte.appendChild(fkElem);
            Element fkattribute = doc.createElement(XMLConstants.FOREIGN_KEY_ATTRIBUTE_TAG);
            fkattribute.setTextContent(key);
            fkElem.appendChild(fkattribute);
            Element reference=doc.createElement(XMLConstants.REFERENCES_TAG);
            fkElem.appendChild(reference);
            Element refTable=doc.createElement(XMLConstants.REF_TABLE_TAG);
            refTable.setTextContent(column.getFKeys().get(key).split(",")[0]);
            reference.appendChild(refTable);
            Element refAttr=doc.createElement(XMLConstants.REF_ATTRIBUTE_TAG);
            refAttr.setTextContent(column.getFKeys().get(key).split(",")[1]);
            reference.appendChild(refAttr);
        }
        return fkElemParinte;
    }

    public void dropTable(String dbName, String tableName) {

        NodeList tableList = doc.getElementsByTagName(XMLConstants.TABLE_TAG);

        boolean isDeleted=false;
        for(int i=0; i<tableList.getLength(); i++){
            Element currentTable = (Element)tableList.item(i);
            Element currentDb = (Element) currentTable.getParentNode().getParentNode();

            if(currentTable.getAttribute(XMLConstants.TABLE_NAME_TAG).equals(tableName)
            && currentDb.getAttribute(XMLConstants.NAME_TAG).equals(dbName)){
                currentTable.getParentNode().removeChild(currentTable);
                isDeleted=true;
            }
        }
        
//        if (isDeleted==false){
//            throw new ServiceException("There is no table in this database with this name ",ExceptionType.DATABASE_OR_TABLE_NOT_EXISTS,HttpStatus.BAD_REQUEST);
//        }

        submitChangesToFile();
    }

    /**add a new index for a table*/
    @Override
    public void addIndex(String dbName, String tableName, Index index) {
        String fileName = index.getColumnList().stream()
                .map(Column::getAttributeName)
                .collect(Collectors.joining(""));
        fileName += "_" + dbName + "_" + tableName;
        index.setFilename(fileName);


        Element indexFilesElement = doc.createElement(XMLConstants.INDEX_FILES_TAG);
        Element indexFileElement = doc.createElement(XMLConstants.INDEX_FILES_TAG);
        indexFileElement.setAttribute(XMLConstants.INDEX_NAME_TAG, index.getName());
        indexFileElement.setAttribute(XMLConstants.IS_UNIQUE_TAG, String.valueOf(index.getIsUnique()));
        indexFileElement.setAttribute(XMLConstants.INDEX_TYPE_TAG, index.getIndexType());
        indexFileElement.setAttribute(XMLConstants.INDEX_FILES_TAG,fileName);

        indexFilesElement.appendChild(indexFileElement);

        Element indexAttributes = doc.createElement(XMLConstants.INDEX_ATTRIBUTES_TAG);
        indexFileElement.appendChild(indexAttributes);

        for (int i=0;i<index.getColumnList().size();i++){
            Element iAttributeElement = doc.createElement(XMLConstants.I_ATTRIBUTE_TAG);
            iAttributeElement.setTextContent(index.getColumnList().get(i).getAttributeName());

            indexAttributes.appendChild(iAttributeElement);
        }


        //append to the table with tableName in the database dbName
        NodeList tableList = doc.getElementsByTagName(XMLConstants.TABLE_TAG);

        for(int i=0; i<tableList.getLength(); i++){
            Element currentTable = (Element)tableList.item(i);
            Element currentDb = (Element) currentTable.getParentNode().getParentNode();

            if(currentTable.getAttribute(XMLConstants.TABLE_NAME_TAG).equals(tableName)
                    && currentDb.getAttribute(XMLConstants.NAME_TAG).equals(dbName)){
                currentTable.appendChild(indexFilesElement);
                break;
            }
        }

        submitChangesToFile();

    }

    /**get all databases*/
    @Override
    public List<String> getAllDatabase() {

        List<String> allDb=new ArrayList<>();
        NodeList nodeList = doc.getElementsByTagName(XMLConstants.DATABASE_TAG);

        for(int i=0; i<nodeList.getLength(); i++){
            Element currentElement = (Element)nodeList.item(i);
            String name=currentElement.getAttribute(XMLConstants.NAME_TAG);
            allDb.add(name);
        }
        return allDb;

    }

    /**get all name columns for a specific table*/
    @Override
    public List<String> getAllColumnNameForTable(String dbName,String tableName) {
        List<String> allCols=new ArrayList<>();
        NodeList tableList = doc.getElementsByTagName(XMLConstants.TABLE_TAG);

        for(int i=0; i<tableList.getLength(); i++){
            Element currentTable = (Element)tableList.item(i);
            Element currentDb = (Element) currentTable.getParentNode().getParentNode();

            if(currentTable.getAttribute(XMLConstants.TABLE_NAME_TAG).equals(tableName)
                    && currentDb.getAttribute(XMLConstants.NAME_TAG).equals(dbName)) {
                    // find table
                NodeList attrList=currentTable.getElementsByTagName("Attribute");
                for (int j=0;j<attrList.getLength();j++){
                    Element elem=(Element)attrList.item(j);
                    String nameAttr=elem.getAttribute("attributeName");
                    allCols.add(nameAttr);
                }
            }
        }
        return allCols;

    }

    /**get all columns for a specific table*/
    @Override
    public List<Column> getAllColumnForTable(String dbName, String tableName) {
        List<Column> allCols=new ArrayList<>();
        NodeList tableList = doc.getElementsByTagName(XMLConstants.TABLE_TAG);

        for(int i=0; i<tableList.getLength(); i++){
            Element currentTable = (Element)tableList.item(i);
            Element currentDb = (Element) currentTable.getParentNode().getParentNode();

            if(currentTable.getAttribute(XMLConstants.TABLE_NAME_TAG).equals(tableName)
                    && currentDb.getAttribute(XMLConstants.NAME_TAG).equals(dbName)) {
                // find table
                NodeList attrList=currentTable.getElementsByTagName(XMLConstants.ATTRIBUTE_TAG);
                NodeList fks=currentTable.getElementsByTagName(XMLConstants.FOREIGN_KEY_TAG);
                String typeAttr="";
                String nameAttr="";
                Integer length=0;
                boolean isNull=false;
                boolean isPrimaryKey=false;
                boolean isUnique=false;
                for (int j=0;j<attrList.getLength();j++){
                    Element elem=(Element)attrList.item(j);
                    nameAttr=elem.getAttribute(XMLConstants.ATTRIBUTE_NAME_TAG);
                    typeAttr=elem.getAttribute(XMLConstants.TYPE_TAG);
                    length= Integer.valueOf(elem.getAttribute(XMLConstants.LENGTH_TAG));

                    isNull= Boolean.parseBoolean(elem.getAttribute(XMLConstants.IS_NULL_TAG));
                    isPrimaryKey=false;
                    if (elem.hasAttribute(XMLConstants.PRIMARY_KEY_TAG)){
                        isPrimaryKey=true;
                    }
                    isUnique=false;
                    if (elem.hasAttribute(XMLConstants.IS_UNIQUE_TAG)){
                        isUnique=true;
                    }
                }

                Map<String,String> mapfk=new HashMap<>();
                for (int j=0;j<fks.getLength();j++){
                    Element e=(Element)fks.item(j);
                    NodeList nodeList=e.getElementsByTagName(XMLConstants.FOREIGN_KEY_ATTRIBUTE_TAG);
                    String key= nodeList.item(0).getFirstChild().getNodeValue();
                    NodeList nodeListTable=e.getElementsByTagName(XMLConstants.REF_TABLE_TAG);
                    String value= nodeListTable.item(0).getFirstChild().getNodeValue();
                    NodeList nodeListRef=e.getElementsByTagName(XMLConstants.REF_ATTRIBUTE_TAG);
                    value= value+","+nodeListRef.item(0).getFirstChild().getNodeValue();
                    mapfk.put(key,value);
                }
                Column col=new Column(nameAttr,typeAttr,length,isNull,isPrimaryKey,isUnique,mapfk);
                allCols.add(col);
            }
        }
        return allCols;
    }

    /**get all tables for a database*/
    @Override
    public List<String> getAllTablesForDb(String dbName) {
        List<String> allTables=new ArrayList<>();
        NodeList tableList = doc.getElementsByTagName(XMLConstants.TABLE_TAG);

        for(int i=0; i<tableList.getLength(); i++){
            Element currentTable = (Element)tableList.item(i);
            Element currentDb = (Element) currentTable.getParentNode().getParentNode();

            if (currentDb.getAttribute(XMLConstants.NAME_TAG).equals(dbName)) {
               allTables.add(currentTable.getAttribute("tableName"));
            }
        }
        return allTables;
    }

    /**get all tables from a database
     to establish a relationship with the table "table"
     */
    @Override
    public Map<String,String> getAnotherTablesForDb(String dbName,String table) {
        Map<String,String> allTables=new HashMap<>(); //map where key is name of the table and value name of the primary key for the table
        NodeList tableList = doc.getElementsByTagName(XMLConstants.TABLE_TAG);

        for(int i=0; i<tableList.getLength(); i++){
            Element currentTable = (Element)tableList.item(i);
            Element currentDb = (Element) currentTable.getParentNode().getParentNode();
            if (currentDb.getAttribute(XMLConstants.NAME_TAG).equals(dbName) && !currentTable.getAttribute("tableName").equals(table)) {
                String key=currentTable.getAttribute("tableName");
                NodeList nodeListTable=currentTable.getElementsByTagName(XMLConstants.PRIMARY_KEY_ATTRIBUTE_TAG);
                String value= nodeListTable.item(0).getFirstChild().getNodeValue();
                System.out.println(value);

                //  String value= String.valueOf(currentTable.getElementsByTagName(XMLConstants.PRIMARY_KEY_ATTRIBUTE_TAG));
                allTables.put(key,value);
            }
        }

        return allTables;
    }


    /**
     * Applies changes to file
     */
    private void submitChangesToFile() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(FILE_NAME);

        try {
            transformer.transform(domSource, streamResult);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
