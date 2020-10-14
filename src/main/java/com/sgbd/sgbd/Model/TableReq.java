package com.sgbd.sgbd.Model;

import lombok.*;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class TableReq {
    private Column[] cols;
}
