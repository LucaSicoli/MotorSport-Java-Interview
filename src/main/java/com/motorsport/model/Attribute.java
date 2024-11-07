package com.motorsport.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Attribute {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("value_name")
    private String valueName;

    // Getters y Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getValueName() {
        return valueName;
    }
}