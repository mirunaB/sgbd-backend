package com.sgbd.sgbd.model;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Index {
    private String name, filename;
    private Boolean isUnique;
    private String indexType;
    private List<Column> columnList;

    public Index(String name, Boolean isUnique, List<Column> columnList) {
        this.name = name;
        this.isUnique = isUnique;
        this.columnList = columnList;
    }
}
