package com.sgbd.sgbd.model;

import lombok.*;

import java.io.Serializable;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class JoinReq implements Serializable {
    private String table1;
    private String col1;
    private String table2;
    private String col2;
    private String selectedColumns;
    private String condition;
}


