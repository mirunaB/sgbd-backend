package com.sgbd.sgbd.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class TableReq {
    private Column[] cols;
}
