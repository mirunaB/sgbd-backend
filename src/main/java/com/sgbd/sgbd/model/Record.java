package com.sgbd.sgbd.model;

import lombok.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Record implements Serializable {

    private Map<Column, String> row;

    public Set<String> getPrimaryKeySet() {

        return row.keySet().stream()
                .filter(column -> column.isPrimaryKey())
                .map(column -> column.attributeName)
                .collect(Collectors.toSet());
    }
}
