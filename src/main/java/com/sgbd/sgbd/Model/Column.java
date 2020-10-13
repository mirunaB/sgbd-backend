package com.sgbd.sgbd.Model;

import lombok.*;

import java.io.Serializable;

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

    //index props: TODO: consider moving this to an other class
    boolean hasIndex;
    String indexName;
    int keyLength;
    boolean isUnique;
    String indexType;
}
