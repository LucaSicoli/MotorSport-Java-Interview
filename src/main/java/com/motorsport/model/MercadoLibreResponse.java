package com.motorsport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MercadoLibreResponse {

    @JsonProperty("results")
    private List<Item> results;

    public List<Item> getResults() {
        return results;
    }
}