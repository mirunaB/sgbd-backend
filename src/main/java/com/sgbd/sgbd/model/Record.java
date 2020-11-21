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
public class Record implements Serializable {

    private Map<String,String> row;


}
