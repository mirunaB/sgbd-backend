package com.sgbd.sgbd.model;

import lombok.*;

import java.io.Serializable;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Column implements Serializable {

    String attributeName;
    String type;
    Integer length;
    boolean isNull;
    boolean isPrimaryKey;
    boolean isUniqueKey;
    private Map<String,String> fKeys;

    public Map<String, String> getFKeys() {
        return fKeys;
    }

    public boolean getIsNull(){
        return isNull;
    }

    public void setIsNull(boolean isNull){
        this.isNull=isNull;
    }

    public void setfKeys(Map<String,String> keys){
        this.fKeys=keys;
    }

    public boolean getIsPrimaryKey(){
        return isPrimaryKey;
    }

    public void setIsPrimaryKey(boolean pk){
        this.isPrimaryKey=pk;
    }

    public void setIsUniqueKey(boolean uniqueKey){
        this.isUniqueKey=uniqueKey;
    }

    public Boolean getIsUniqueKey(){
        return isUniqueKey;
    }
}
